import {OverlayConfig} from "@angular/cdk/overlay";
import {CdkPortalOutlet, ComponentPortal} from "@angular/cdk/portal";
import {ScrollDispatcher} from "@angular/cdk/scrolling";
import {ChangeDetectionStrategy, Component, ComponentRef, ElementRef, EventEmitter, Injector, Input, Output, ViewChild, ViewContainerRef} from "@angular/core";
import {MatMenuTrigger} from "@angular/material/menu";

import {CONTAINER_DATA, CONTAINER_PTR, WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {WidgetExportInfo} from "app/dashboard/dashboard/widgets/widget-export-info";
import {EditWidgetRequest} from "app/dashboard/dashboard/widgets/widget-manager.component";
import {WidgetManipulator, WidgetOutlineExtended} from "app/dashboard/dashboard/widgets/widget-manipulator";
import {NAVBAR_HEIGHT} from "app/layouts/standard-layout.component";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {DashboardConfigurationExtended, WidgetGraph} from "app/services/domain/dashboard-management.service";
import {WidgetConfigurationExtended} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {ChartHelpers} from "framework/ui/charting/app-charting-utilities";
import {ChartBox} from "framework/ui/charting/core/basics";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {OverlayInstance, OverlayService} from "framework/ui/overlays/overlay.service";
import {RoutableViewPortalComponent, RoutableViewSourceDirective} from "framework/ui/routable-view/routable-view-portal.component";
import {Future} from "framework/utils/concurrency";
import {AsyncDebouncer} from "framework/utils/debouncers";

@Component({
               selector       : "o3-widget-container",
               templateUrl    : "./widget-container.component.html",
               styleUrls      : ["./widget-container.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class WidgetContainerComponent extends BaseApplicationComponent
{
    public static readonly SHOW_TOOLBAR_BASE_DELAY: number = 1000;
    public static readonly HIDE_TOOLBAR_BASE_DELAY: number = 600;

    @Input() readonly: boolean      = false;
    @Input() primaryColor: string   = DashboardConfigurationExtended.defaultPrimaryColor;
    @Input() secondaryColor: string = DashboardConfigurationExtended.defaultSecondaryColor;
    @Input() shadowless: boolean    = false;
    @Input() preview: boolean       = false;
    @Input() manipulator: WidgetManipulator;

    private m_isSubwidget: boolean;
    @Input() set isSubwidget(isSubwidget: boolean)
    {
        if (this.m_alwaysShowToolbar == null) this.m_alwaysShowToolbar = !isSubwidget;
        this.m_isSubwidget = isSubwidget;
    }

    get isSubwidget(): boolean
    {
        return this.m_isSubwidget;
    }

    private m_initialized: Future<void>;
    private m_bound: Future<void>;

    get bound(): boolean
    {
        return !!this.m_bound?.isResolved();
    }

    @Input() set model(model: Models.WidgetConfiguration)
    {
        if (!model) return;

        this.m_initialized = new Future();
        this.init(model);
    }

    @Input() outline: WidgetOutlineExtended;

    private m_focus: boolean = false;
    @Input() set focus(isFocus: boolean)
    {
        if (this.m_focus !== isFocus)
        {
            this.m_focus = isFocus;
            if (this.widget) this.widget.focus = this.m_focus;
        }
    }

    get focus(): boolean
    {
        return this.m_focus;
    }

    get relatedToFocus(): boolean
    {
        return !!this.widget?.relatedToFocus;
    }

    get alwaysShowToolbar(): boolean
    {
        return this.m_alwaysShowToolbar;
    }

    get showingToolbar(): boolean
    {
        if (this.preview) return false;

        return this.alwaysShowToolbar || !!this.maximizedOverlay || this.isShowingHiddenToolbar;
    }

    get isShowingHiddenToolbar(): boolean
    {
        return !!this.m_toolbarOverlay;
    }

    public canCollapse: boolean = false;

    private m_isCollapsed: boolean        = false;
    private m_isCollapsedDelayed: boolean = false;

    get isCollapsed(): boolean
    {
        return this.m_isCollapsed;
    }

    get isCollapsedDelayed(): boolean
    {
        return this.m_isCollapsedDelayed;
    }

    public collapseWidget()
    {
        this.m_isCollapsed = !this.m_isCollapsed;
        if (this.m_isCollapsed)
        {
            this.m_isCollapsedDelayed = true;
            this.manipulator.updateViewport(false);
        }
        else
        {
            setTimeout(() =>
                       {
                           this.m_isCollapsedDelayed = this.m_isCollapsed;
                           this.markForCheck();
                           this.widget?.markForCheck();
                       }, 20);
        }

        this.manipulator.refresh(false);
        this.markForCheck();
    }

    private m_rendering: boolean = false;
    @Input() set rendering(rendering: boolean)
    {
        this.m_rendering = rendering;
        if (this.widget)
        {
            this.widget.inView = rendering;
        }

        if (rendering)
        {
            if (this.m_bound)
            {
                this.refreshWidget(5);
            }
            else
            {
                this.load();
            }
        }
    }

    private m_baseFont: number;
    @Input() set baseFont(size: number)
    {
        this.m_baseFont = size;
        if (this.widget) this.widget.baseFont = this.m_baseFont;
    }

    get interactableContent(): boolean
    {
        if (this.editing) return !this.m_editingContentProtector;
        if (this.preview) return false;
        return this.focus || !this.m_needsContentProtector;
    }

    private m_manipulating: boolean = false;
    @Input() set manipulating(manipulating: boolean)
    {
        this.processStateChange(() => this.m_manipulating = manipulating, true);
    }

    @ViewChild("widgetScrollContainer", {static: true}) widgetScrollContainer: ElementRef;
    @ViewChild(CdkPortalOutlet, {static: true}) private widgetContents: CdkPortalOutlet;
    @ViewChild(RoutableViewSourceDirective, {static: true}) view: RoutableViewSourceDirective;
    @ViewChild("widgetPortal",
               {
                   static: true,
                   read  : ElementRef
               }) private widgetPortal: ElementRef<any>;
    @ViewChild("contextMenuTriggerWrapper", {static: true}) contextMenuTrigger: MatMenuTrigger;
    @ViewChild("contextMenuTriggerWrapper", {
        read  : ElementRef,
        static: true
    }) contextMenuTriggerElement: ElementRef;

    @ViewChild("test_menuTrigger", {read: ElementRef}) test_menuTrigger: ElementRef;

    widget: WidgetBaseComponent<any, any>;

    maximizedOverlay: OverlayInstance<ComponentRef<RoutableViewPortalComponent>>;

    private m_needsContentProtector: boolean   = true;
    private m_editingContentProtector: boolean = true;

    configExt: WidgetConfigurationExtended<Models.WidgetConfiguration>;
    contentClases: string[] = [];

    showToolbarTitle: boolean = true;
    private m_toolbarOverlay: OverlayInstance<ComponentRef<RoutableViewPortalComponent>>;
    private m_toolbarOverlayConfig: OverlayConfig;
    private m_alwaysShowToolbar: boolean;
    private m_alwaysHideToolbar: boolean;

    editing: boolean               = false;
    loading: boolean               = false;
    private m_usingMenu: boolean   = false;
    private m_mousemove: MouseEvent;
    private m_adaptiveHover: AdaptiveHoverToolbarManager;
    private m_maximizable: boolean = true;
    private m_contentDomRect: DOMRect;

    private set mouseMove(mousemove: MouseEvent)
    {
        this.processStateChange(() =>
                                {
                                    this.m_mousemove = mousemove;
                                    this.widgetHovering.emit(!!this.m_mousemove);
                                }, false);
    }

    get relatedToFocusTooltip(): string
    {
        if (this.widget?.isContextPublisher())
        {
            return "Changing the selected asset affects the focused widget's content";
        }
        else
        {
            return "This widget's content will change if you change the focused widget's asset";
        }
    }

    get maximizable(): boolean
    {
        return this.m_maximizable;
    }

    scrollDebouncer = new AsyncDebouncer(100, async () => this.widget.scrollTop = this.widgetScrollContainer.nativeElement.scrollTop);

    @Output() widgetNameUpdated    = new EventEmitter<void>();
    @Output() widgetContentUpdated = new EventEmitter<void>();
    @Output() widgetHovering       = new EventEmitter<boolean>();
    @Output() widgetEdit           = new EventEmitter<EditWidgetRequest>();
    @Output() widgetDelete         = new EventEmitter<WidgetGraph>();
    @Output() cursorClassChange    = new EventEmitter<string[]>();

    get element(): ElementRef
    {
        return this.m_element;
    }

    get isElevated(): boolean
    {
        return !!this.m_mousemove || this.m_usingMenu || this.focus;
    }

    constructor(inj: Injector,
                private m_element: ElementRef,
                private m_overlayService: OverlayService,
                private m_viewContainer: ViewContainerRef,
                private m_scrollDispatcher: ScrollDispatcher)
    {
        super(inj);

        this.editing = this.app.domain.dashboard.isEditing.getValue();
        this.subscribeToObservable(this.app.domain.dashboard.isEditing, (editing) =>
        {
            if (editing !== this.editing)
            {
                this.editing = editing;
                if (this.widget) this.widget.editing = editing;
                if (this.editing && this.isCollapsed) this.collapseWidget();
                this.markForCheck();
            }
        });

        this.subscribeToObservable(this.m_scrollDispatcher.scrolled(), () => this.m_contentDomRect = null);
    }

    public ngOnDestroy()
    {
        super.ngOnDestroy();

        this.cancelAdaptiveHover();
        if (this.isShowingHiddenToolbar) this.disposeToolbarOverlay();
        if (this.maximizedOverlay) this.minimize();
    }

    public getDesiredHeight(): number
    {
        if (this.isCollapsed)
        {
            return this.app.css.WidgetToolbarHeight.asNumber + this.app.css.DashboardWidgetPadding.asNumber * 2;
        }

        let desiredHeight = this.widget?.getDesiredSize() || null;

        if (desiredHeight != null)
        {
            if (this.showingToolbar) desiredHeight += this.app.css.WidgetToolbarHeight.asNumber;

            desiredHeight += this.app.css.DashboardWidgetPadding.asNumber * 2;
        }

        return desiredHeight;
    }

    private init(model: Models.WidgetConfiguration)
    {
        switch (model.toolbarBehavior)
        {
            case Models.WidgetToolbarBehavior.AlwaysShow:
                this.m_alwaysShowToolbar = true;
                break;

            case Models.WidgetToolbarBehavior.Collapsible:
                this.m_alwaysShowToolbar = true;
                this.canCollapse         = true;
                break;

            case Models.WidgetToolbarBehavior.AutoHide:
                this.m_alwaysShowToolbar = false;
                break;

            case Models.WidgetToolbarBehavior.Hide:
                this.m_alwaysShowToolbar = false;
                this.m_alwaysHideToolbar = true;
                break;
        }

        this.configExt = WidgetConfigurationExtended.fromConfigModel(model);

        let descriptor                 = this.configExt.getDescriptor().config;
        this.m_maximizable             = descriptor.maximizable;
        this.m_needsContentProtector   = descriptor.needsProtector;
        this.m_editingContentProtector = descriptor.model !== Models.GroupingWidgetConfiguration;

        this.contentClases = ["widget"];
        if (descriptor.classes)
        {
            this.contentClases.push(...descriptor.classes);
        }

        this.m_element.nativeElement.id = `widget-${model.id}`;

        this.detectChanges();

        if (!this.wasDestroyed())
        {
            if (!this.widget)
            {
                let portal   = new ComponentPortal(descriptor.component, null, this.createInjector());
                let attached = portal.attach(this.widgetContents);
                this.widget  = attached.instance;
            }
            else
            {
                this.widget.configExt.model = model;
            }

            this.widget.editing  = this.editing;
            this.widget.baseFont = this.m_baseFont;
            this.widget.focus    = this.m_focus;
        }

        this.m_initialized.resolve();
    }

    public async load()
    {
        this.loading = true;
        await this.m_initialized;

        if (this.m_bound || this.wasDestroyed() || !this.m_rendering)
        {
            this.loading = false;
            return;
        }

        this.m_bound = new Future();

        this.subscribeToObservable(this.widget.contentUpdated, () => this.widgetContentUpdated.emit());
        this.subscribeToObservable(this.widget.dimensionsChanged, () => this.updateToolbarOverlayConfig());
        this.subscribeToObservable(this.widget.widgetEdit, (event) => this.widgetEdit.emit(event));
        this.subscribeToObservable(this.widget.widgetDelete, (graph) => this.widgetDelete.emit(graph));
        this.subscribeToObservable(this.widget.cursorClassChange, (classes) => this.cursorClassChange.emit(classes));
        this.subscribeToObservable(this.widget.loadingChanged, (loading) =>
        {
            this.loading = loading;
            this.detectChanges();
        });

        await this.widget.load();
        this.widget.inView = true;

        this.m_bound.resolve();

        this.refreshWidget(5);

        this.detectChanges();
    }

    private canFloatToolbar(): boolean
    {
        if (!this.m_toolbarOverlayConfig) return false;
        if (this.maximizedOverlay) return false;
        if (this.m_alwaysShowToolbar) return false;
        if (this.m_alwaysHideToolbar && !this.editing) return false;
        if (this.m_manipulating) return false;
        if (this.preview) return false;

        if (!this.m_contentDomRect) this.m_contentDomRect = this.widgetScrollContainer.nativeElement.getBoundingClientRect();
        if (this.m_contentDomRect.top < NAVBAR_HEIGHT) return false;

        return !!this.m_mousemove || this.m_usingMenu;
    }

    private updateToolbarOverlayConfig()
    {
        if (!this.m_alwaysShowToolbar)
        {
            if (this.isShowingHiddenToolbar)
            {
                this.disposeToolbarOverlay();
            }

            const minWidth        = 40;
            this.showToolbarTitle = this.widget.showToolbarTitle;

            if (this.widget.widthRaw < minWidth)
            {
                this.showToolbarTitle = false;
            }

            this.m_toolbarOverlayConfig = new OverlayConfig({
                                                                hasBackdrop   : false,
                                                                scrollStrategy: this.app.ui.overlay.scrollStrategies.close()
                                                            });

            this.markForCheck();
        }
    }

    private processStateChange(updateFn: () => void,
                               noHideDelay: boolean)
    {
        const prevFloat = this.canFloatToolbar();

        updateFn();

        this.updateAdaptiveHover(prevFloat, noHideDelay);
    }

    private updateAdaptiveHover(prevFloat: boolean,
                                noHideDelay: boolean)
    {
        const float = this.canFloatToolbar();
        if (prevFloat !== float)
        {
            this.cancelAdaptiveHover();

            const baseDelay = float ?
                WidgetContainerComponent.SHOW_TOOLBAR_BASE_DELAY :
                (noHideDelay ? 0 : WidgetContainerComponent.HIDE_TOOLBAR_BASE_DELAY);

            this.m_adaptiveHover = new AdaptiveHoverToolbarManager(this, baseDelay, this.m_mousemove);
        }
        else if (this.m_adaptiveHover)
        {
            this.m_mousemove ?
                this.m_adaptiveHover.handleMousemove(this.m_mousemove) :
                this.m_adaptiveHover.handleMouseout();
        }
    }

    private cancelAdaptiveHover()
    {
        if (this.m_adaptiveHover)
        {
            this.m_adaptiveHover.cancel();
            this.m_adaptiveHover = null;
        }
    }

    public toggleToolbar()
    {
        if (this.canFloatToolbar() !== this.isShowingHiddenToolbar)
        {
            if (this.isShowingHiddenToolbar)
            {
                this.disposeToolbarOverlay();
            }
            else
            {
                let portal = new ComponentPortal(RoutableViewPortalComponent);

                this.m_toolbarOverlayConfig.positionStrategy = this.m_overlayService.generateTrackedStrategy(this.widgetPortal.nativeElement, null, null);

                this.m_toolbarOverlay = this.m_overlayService.createComponentOverlay(this.m_toolbarOverlayConfig, portal, {
                    dismissOnBackdrop: true,
                    dismissOnEscape  : true,
                    dismissOnNavigate: true,
                    afterAttach      : (ref) =>
                    {
                        // Immediately re-route view on attach
                        ref.portal.instance.source = this.view;
                        ref.portal.instance.capture();
                        this.widget.markForCheck();
                    },
                    afterDispose     : (ref) =>
                    {
                        // Clear overlay state once disposed
                        ref.portal.instance.release();
                        this.widget.markForCheck();
                    }
                });
            }
        }
    }

    private disposeToolbarOverlay()
    {
        if (this.m_toolbarOverlay)
        {
            this.m_toolbarOverlay.dispose();
            this.m_toolbarOverlay = null;
        }
    }

    public onMouseMove(event: MouseEvent,
                       fromContentProtector: boolean)
    {
        this.mouseMove = event;

        if (fromContentProtector && !this.editing && !this.focus && this.widget && !this.widget.isLoading)
        {
            this.widget.onMouseMove(event, this.widgetScrollContainer.nativeElement.scrollTop);
        }
    }

    public onMouseLeave()
    {
        this.mouseMove = null;

        if (!this.focus && this.widget)
        {
            this.widget.onMouseLeave();
        }
    }

    public editWidget()
    {
        let widgetComposition = Models.WidgetComposition.newInstance({
                                                                         outline: this.outline.model,
                                                                         config : this.configExt.model
                                                                     });
        this.widgetEdit.emit(new EditWidgetRequest(widgetComposition));
    }

    public async export()
    {
        const exportable = await this.widget.generateClipboardData();
        const outline    = Models.WidgetOutline.newInstance(this.outline.model);
        const exportInfo = new WidgetExportInfo(exportable.getDashboardWidget(), outline, exportable.selectors, exportable.selectorGraphs);

        const widgetName = this.widget.config.name;
        const fileName   = DownloadDialogComponent.fileName(`dashboard_widget${widgetName ? "__" + widgetName : ""}`);
        DownloadDialogComponent.open(this, "Widget Export", fileName, exportInfo);
    }

    public async refreshWidget(numRetries: number = 0): Promise<boolean>
    {
        if (this.m_bound)
        {
            await this.m_bound;
            return UtilsService.executeWithRetries(() => this.refreshWidgetHelper(), numRetries, 200, undefined, 2, true);
        }

        return false;
    }

    public cut()
    {
        if (this.widget.removable)
        {
            this.widget.copy();
            this.widget.remove();
        }
    }

    private async refreshWidgetHelper(): Promise<boolean>
    {
        if (this.wasDestroyed()) return true;

        if (this.m_rendering && this.widget)
        {
            this.setDimensions();
            await this.widget.refresh();
        }

        return this.widget?.loaded;
    }

    public onMenuTrigger()
    {
        this.processStateChange(() => this.m_usingMenu = !this.contextMenuTrigger.menuOpen, false);
    }

    public onMenuClose()
    {
        this.processStateChange(() => this.m_usingMenu = false, false);
    }

    private setDimensions(): void
    {
        let bounding = this.widgetScrollContainer.nativeElement.getBoundingClientRect();
        this.widget.setDimensions(bounding.width, bounding.height);
    }

    private createInjector()
    {
        return Injector.create({
                                   providers: [
                                       {
                                           provide : CONTAINER_DATA,
                                           useValue: {configExt: this.configExt}
                                       },
                                       {
                                           provide : CONTAINER_PTR,
                                           useValue: this
                                       }
                                   ],
                                   parent   : this.injector
                               });
    }

    public maximize()
    {
        if (this.isShowingHiddenToolbar)
        {
            this.disposeToolbarOverlay();
        }

        // Get source and miximize container DOM elements
        let maximizeContainer: HTMLElement = document.body;
        let sourceContainer: HTMLElement   = this.m_element.nativeElement;

        // Configure and spawn maximize overlay
        let portal            = new ComponentPortal(RoutableViewPortalComponent);
        let strategy          = this.m_overlayService.generateMaximizeStrategy(sourceContainer, maximizeContainer, 0.95, 500);
        let cfg               = {
            hasBackdrop     : true,
            positionStrategy: strategy
        };
        this.maximizedOverlay = this.m_overlayService.createComponentOverlay(cfg, portal, {
            dismissOnBackdrop   : true,
            dismissOnEscape     : true,
            dismissOnNavigate   : true,
            afterAttach         : (ref) =>
            {
                // Immediately re-route view on attach
                ref.portal.instance.source = this.view;
                ref.portal.instance.capture();
            },
            afterAttachAnimation: (ref) =>
            {
                // Trigger digest and size update when fullscreen animation is settled
                this.refreshWidget();
            },
            afterDetachAnimation: (ref) =>
            {
                // Return view and clean up overlay and digest when detach animation is finished
                ref.portal.instance.release();
                this.maximizedOverlay = null;
                this.markForCheck();
            },
            afterDispose        : (ref) =>
            {
                this.refreshWidget();
            }
        });
    }

    public minimize()
    {
        // Close/dispose the overlay
        if (this.maximizedOverlay) this.maximizedOverlay.dispose();
    }
}

class AdaptiveHoverToolbarManager
{
    private static readonly STATIC_INTERVAL_MS: number = 20;

    private readonly m_showing: boolean = false;

    private m_adaptiveMultiplier: number = 1;
    private m_resolved: boolean          = false;

    constructor(public readonly host: WidgetContainerComponent,
                public readonly baseTimeout: number,
                hover: MouseEvent)
    {
        if (hover)
        {
            this.m_showing = true;
            this.handleMousemove(hover);
        }

        this.startAdaptiveTimeout();
    }

    private async startAdaptiveTimeout()
    {
        const start = performance.now();
        while (!this.m_resolved && !this.host.wasDestroyed())
        {
            const multiplier = this.host.editing ? this.m_adaptiveMultiplier : 1;
            if (multiplier * (performance.now() - start) > this.baseTimeout)
            {
                this.complete();
            }
            else
            {
                await Future.delayed(AdaptiveHoverToolbarManager.STATIC_INTERVAL_MS);
            }
        }
    }

    public handleMousemove(event: MouseEvent)
    {
        if (this.m_showing)
        {
            const widgetRect     = this.host.element.nativeElement.getBoundingClientRect();
            const closeToToolbar = new ChartBox(widgetRect.left, widgetRect.top, widgetRect.width, 50).hitCheck(event.clientX, event.clientY);
            const closeToMenu    = ChartHelpers.pointDistance(widgetRect.right - 20, widgetRect.top + 20, event.clientX, event.clientY) < 35;

            this.m_adaptiveMultiplier = 1 / 3;
            if (closeToToolbar) this.m_adaptiveMultiplier *= 3;
            if (closeToMenu) this.m_adaptiveMultiplier *= 2;
        }
        else
        {
            this.cancel();
        }
    }

    public handleMouseout()
    {
        if (this.m_showing) this.cancel();
    }

    private complete()
    {
        if (!this.m_resolved)
        {
            this.m_resolved = true;

            this.host.toggleToolbar();
            this.host.markForCheck();
        }
    }

    public cancel()
    {
        this.m_resolved = true;
    }
}
