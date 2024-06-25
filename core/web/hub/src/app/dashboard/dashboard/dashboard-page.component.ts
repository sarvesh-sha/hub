import {CDK_DRAG_CONFIG} from "@angular/cdk/drag-drop";
import {BreakpointObserver, Breakpoints} from "@angular/cdk/layout";
import {CdkScrollable} from "@angular/cdk/overlay";
import {ChangeDetectionStrategy, Component, ElementRef, HostListener, Injector, OnDestroy, ViewChild} from "@angular/core";
import {Event, NavigationStart} from "@angular/router";

import {AppService} from "app/app.service";
import {DashboardBannerComponent} from "app/dashboard/dashboard/dashboard-banner.component";
import {WidgetImportDialog} from "app/dashboard/dashboard/widgets/widget-import-dialog";
import {EditWidgetRequest} from "app/dashboard/dashboard/widgets/widget-manager.component";
import {WidgetLayoutConfig, WidgetManipulator} from "app/dashboard/dashboard/widgets/widget-manipulator";
import {WidgetEditorWizardDialogComponent, WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as SharedSvc from "app/services/domain/base.service";
import {ClipboardEntryData, ClipboardService} from "app/services/domain/clipboard.service";
import {DashboardDefinitionVersionExtended} from "app/services/domain/dashboard-definition-versions.service";
import {DashboardConfigurationExtended, DashboardManagementService, WidgetGraph, WidgetNode} from "app/services/domain/dashboard-management.service";
import {WidgetConfigurationExtended} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {StateRestorable, StateSnapshot} from "app/shared/undo/undo-redo-state";
import {UndoRedoComponent} from "app/shared/undo/undo-redo.component";
import {VersionDraft, VersionManager} from "app/shared/undo/version-manager";

import {UtilsService} from "framework/services/utils.service";
import {SelectComponent} from "framework/ui/forms/select.component";
import {MatIconSize} from "framework/ui/layout";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {ScrollerConfig} from "framework/ui/utils/drag-scroller";
import {Future, mapInParallel} from "framework/utils/concurrency";
import {SyncDebouncer} from "framework/utils/debouncers";
import {getHotkeyAction, HotkeyAction} from "framework/utils/keyboard-hotkeys";

@Component({
               selector       : "o3-dashboard-page",
               templateUrl    : "./dashboard-page.component.html",
               styleUrls      : ["./dashboard-page.component.scss"],
               providers      : [
                   {
                       provide : CDK_DRAG_CONFIG,
                       useValue: {
                           dragStartThreshold             : 2,
                           pointerDirectionChangeThreshold: 5
                       }
                   }
               ],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class DashboardPageComponent extends SharedSvc.BaseApplicationComponent implements StateRestorable<DashboardConfigurationExtended>,
                                                                                          OnDestroy
{
    private static readonly DASHBOARD_WIDGET_MANAGER_ID = "<root>";

    readonly editBannerIconSize  = MatIconSize.medium;
    assetStructuresOverlayConfig = OverlayConfig.newInstance({
                                                                 containerClasses   : ["dialog-xl"],
                                                                 closableViaBackdrop: false,
                                                                 showCloseButton    : true
                                                             });

    versionStateResolved = false;

    titleChangedDebouncer = new SyncDebouncer(1000, () => this.checkRecordTitleChange());
    private dashboardTitle: string;
    readonly versionManager: VersionManager<Models.DashboardDefinitionVersion, DashboardDefinitionVersionExtended, DashboardManagementService, DashboardConfigurationExtended>;

    get activeVersionId()
    {
        return this.versionManager.activeVersionId;
    }

    set activeVersionId(versionId: string)
    {
        if (versionId && versionId !== this.activeVersionId)
        {
            this.updateVersion(versionId); // Don't wait.
        }
    }

    private async updateVersion(versionId: string)
    {
        this.versionStateResolved = false;
        this.versionManager.saveDraft(await this.getBaseSnapshot());

        let verInfo = this.versionManager.getVersionInfo(versionId);
        if (verInfo)
        {
            let version = verInfo.version;
            let draft   = verInfo.draft;

            let dashboardConfig: DashboardConfigurationExtended;
            if (draft)
            {
                dashboardConfig = draft.history[draft.pointer]?.state;
            }

            if (!dashboardConfig)
            {
                dashboardConfig = await this.app.domain.dashboard.getConfigExtended(version);
            }

            await this.restoreToState(dashboardConfig, true, draft);
        }
        this.versionStateResolved = true;
    }

    private async getBaseSnapshot(): Promise<StateSnapshot<DashboardConfigurationExtended>>
    {
        let verExt = this.versionManager.baseVersion;
        let cfgExt = await this.app.domain.dashboard.getConfigExtended(verExt);
        return new StateSnapshot<DashboardConfigurationExtended>(cfgExt, false, `v${verExt.model.version}`);
    }

    private m_cfg: DashboardConfigurationExtended;
    set cfg(cfg: DashboardConfigurationExtended)
    {
        cfg.resolveGraphs(); // Don't wait.

        let model = cfg.model;

        if (!model.widgetPrimaryColor) model.widgetPrimaryColor = DashboardConfigurationExtended.defaultPrimaryColor;
        if (!model.widgetSecondaryColor) model.widgetSecondaryColor = DashboardConfigurationExtended.defaultSecondaryColor;
        if (!model.bannerSegments?.length)
        {
            let branding = Models.BrandingConfiguration.newInstance({
                                                                        text               : model.title,
                                                                        primaryColor       : model.widgetPrimaryColor,
                                                                        secondaryColor     : model.widgetSecondaryColor,
                                                                        horizontalPlacement: Models.HorizontalAlignment.Center,
                                                                        verticalPlacement  : Models.VerticalAlignment.Middle
                                                                    });

            model.bannerSegments = [
                Models.DashboardBannerSegment.newInstance({
                                                              widthRatio: 1,
                                                              branding  : branding
                                                          })
            ];
        }

        this.dashboardTitle = model.title;
        this.updateCfg(cfg);
    }

    get cfg(): DashboardConfigurationExtended
    {
        return this.m_cfg;
    }

    public readonly widgetManipulator: WidgetManipulator;
    private readonly m_dashboardElem: HTMLElement;
    cursorClasses: string[];

    @ViewChild(DashboardBannerComponent) banner: DashboardBannerComponent;

    @ViewChild("test_dashboardName", {read: ElementRef}) test_nameInput: ElementRef;
    @ViewChild("test_undoRedo") test_undoRedo: UndoRedoComponent<DashboardConfigurationExtended>;
    @ViewChild("test_versionSelect") test_versionSelect: SelectComponent<string>;
    @ViewChild("test_editBarMenu", {read: ElementRef}) test_editBarMenu: ElementRef<HTMLElement>;
    @ViewChild("test_addWidget", {read: ElementRef}) test_addWidget: ElementRef;
    @ViewChild("test_cancel", {read: ElementRef}) test_cancel: ElementRef;
    @ViewChild("test_save", {read: ElementRef}) test_save: ElementRef;

    get editing(): boolean
    {
        return this.widgetManipulator.editing;
    }

    private async setEditing(editing: boolean)
    {
        if (editing != this.editing)
        {
            // If entering edit, rebuild state history
            if (editing) await this.rebuildStateHistory();

            this.widgetManipulator.editing = editing;

            if (editing)
            {
                let editAction = this.app.domain.dashboard.enqueuedEdit;
                if (editAction)
                {
                    try
                    {
                        await editAction();
                    }
                    catch (err)
                    {
                        console.error("error executing dashboard edit action: " + err);
                    }
                }
            }

            this.markForCheck();
        }
    }

    get widgetsExist(): boolean
    {
        return !!this.m_cfg?.model.widgets.length;
    }

    get hasPastable()
    {
        for (let data of this.clipboardService.getAll())
        {
            let config = data.getDashboardWidget();
            if (config)
            {
                if (config instanceof Models.TextWidgetConfiguration) continue;

                return true;
            }
        }

        return false;
    }

    @HostListener("document:keydown", ["$event"]) onKeyDown(event: KeyboardEvent)
    {
        if (!this.app.domain.dashboard.widgetWizardOpen)
        {
            // copy, cut (and grouping widget's pasting) are handled inside the widgets
            switch (getHotkeyAction(event))
            {
                case HotkeyAction.Undo:
                    if (this.editing && this.versionManager.stateHistory.canUndo())
                    {
                        this.versionManager.stateHistory.undo();
                    }
                    break;

                case HotkeyAction.Redo:
                    if (this.editing && this.versionManager.stateHistory.canRedo())
                    {
                        this.versionManager.stateHistory.redo();
                    }
                    break;

                case HotkeyAction.Paste:
                    let focusId               = this.widgetManipulator?.focusId;
                    let focusWidget           = this.m_cfg.model.widgets.find((widget) => widget.config.id === focusId);
                    let groupingWidgetFocused = focusWidget?.config instanceof Models.GroupingWidgetConfiguration;
                    if (!groupingWidgetFocused && this.hasPastable)
                    {
                        this.executeEdit(() => this.pasteWidgets());
                    }
                    break;
            }
        }
    }

    private m_mobileView: boolean;
    get mobileView(): boolean
    {
        return this.m_mobileView;
    }

    constructor(inj: Injector,
                scrollable: CdkScrollable,
                elemRef: ElementRef,
                private appService: AppService,
                private clipboardService: ClipboardService,
                breakpointObserver: BreakpointObserver)
    {
        super(inj);

        this.versionManager = new VersionManager(this.app.domain.dashboard);

        let scrollerConfig                 = new ScrollerConfig(1, scrollable);
        let dashboardGridConfig            = new WidgetLayoutConfig(DashboardConfigurationExtended.numDashboardColumns, undefined, undefined, 2);
        this.widgetManipulator             = new WidgetManipulator(this.injector, dashboardGridConfig, scrollerConfig, DashboardPageComponent.DASHBOARD_WIDGET_MANAGER_ID, undefined);
        this.widgetManipulator.aspectRatio = 3;

        this.m_dashboardElem = elemRef.nativeElement;

        this.subscribeToObservable(breakpointObserver.observe(Breakpoints.XSmall), (breakpoint) =>
        {
            this.m_mobileView = breakpoint.matches;
            this.updateWidgetLayout();
            this.markForCheck();
        });
    }

    async ngOnInit()
    {
        await super.ngOnInit();

        this.app.framework.errors.dismiss();

        // init the widgets
        await this.app.domain.dashboard.reloadDashboards(false, false);
        await this.initDashboard();

        this.subscribeToObservable(this.app.domain.dashboard.addWidget, () => this.newWidget());
        this.subscribeToObservable(this.app.domain.dashboard.isEditing, (editing: boolean) => this.setEditing(editing));
        this.subscribeToObservable(this.app.domain.dashboard.activeDashboardChanged, () => this.initDashboard());

        this.subscribeToObservable(this.clipboardService.contentsChanged, () => this.markForCheck());

    }

    ngAfterViewInit()
    {
        super.ngAfterViewInit();

        // subscribe to route change
        this.subscribeToObservable(this.app.routing.router.events, (val: Event) =>
        {
            // if it is a navigation start, cancel editing
            if (val instanceof NavigationStart && this.editing)
            {
                this.app.domain.dashboard.disableEdit();
            }
        });
    }

    protected afterLayoutChange()
    {
        super.afterLayoutChange();

        this.widgetManipulator.refresh();

        this.updateWidgetLayout();
    }

    ngOnDestroy()
    {
        super.ngOnDestroy();

        this.widgetManipulator.destroy();
    }

    private async initDashboard()
    {
        await this.versionManager.initialize();
        await this.versionManager.updateBaseVersion(this.app.domain.dashboard.getHeadIdentity());

        let verExt = this.versionManager.baseVersion;
        let cfg    = await this.app.domain.dashboard.getConfigExtended(verExt);
        this.cfg   = cfg.cloneForEdit();

        this.widgetManipulator.scrollToTop();

        this.versionStateResolved = true;
        this.markForCheck();
    }

    private updateCfg(cfg: DashboardConfigurationExtended)
    {
        cfg.widgetManipulator = this.widgetManipulator;
        this.m_cfg            = cfg;

        this.app.domain.dashboard.currentDashboardConfig.next(cfg);
    }

    //--//

    private requiresColumnarView(): boolean
    {
        return this.widgetManipulator.widgetContainers.some((widgetContainer) => widgetContainer.widget.requiresColumnarView(null, null));
    }

    public updateWidgetLayout()
    {
        let columnarView = this.mobileView || this.requiresColumnarView();
        if (columnarView !== this.widgetManipulator.columnar)
        {
            this.widgetManipulator.columnar = columnarView;
            this.widgetManipulator.updateViewport(false);

            this.markForCheck();
        }
    }

    //--//

    public async restoreToState(cfg: DashboardConfigurationExtended,
                                fromVersionDropdown?: boolean,
                                draft?: VersionDraft<DashboardConfigurationExtended>)
    {
        if (!fromVersionDropdown) this.versionStateResolved = false;

        this.cfg = cfg;
        await this.versionManager.updateBaseVersion(cfg.verExt.getIdentity());

        if (fromVersionDropdown) await this.rebuildStateHistory(draft);
        this.versionManager.updateVersionInfo(fromVersionDropdown);

        if (!fromVersionDropdown) this.versionStateResolved = true;

        this.updateWidgetLayout();
        this.widgetManipulator.refresh(false);
        this.markForCheck();
    }

    private async rebuildStateHistory(draft?: VersionDraft<DashboardConfigurationExtended>)
    {
        const extToSnapshot = async (verExt: DashboardDefinitionVersionExtended) =>
        {
            let cfgExt = await this.app.domain.dashboard.getConfigExtended(verExt);
            return new StateSnapshot(cfgExt, false, `v${verExt.model.version}`);
        };

        let predecessors = await mapInParallel(this.versionManager.getPredecessors(), extToSnapshot);
        let successors   = await mapInParallel(this.versionManager.getSuccessors(), extToSnapshot);

        await this.versionManager.updateStateHistory(this, draft?.baseState || await this.getBaseSnapshot(), draft?.history, draft?.pointer, predecessors, successors);
    }

    public async cloneState(cfgExt: DashboardConfigurationExtended): Promise<DashboardConfigurationExtended>
    {
        return cfgExt.cloneForEdit();
    }

    public async readState(): Promise<DashboardConfigurationExtended>
    {
        return this.m_cfg;
    }

    public async checkRecordTitleChange()
    {
        if (!this.editing) return;

        let model   = this.m_cfg.model;
        model.title = this.app.domain.dashboard.getUniqueDashboardTitle(model.title.trim(), this.m_cfg.dashboardId);
        if (model.title != this.dashboardTitle)
        {
            this.dashboardTitle = model.title;
            await this.recordChange("change dashboard name");
            this.app.domain.dashboard.currentDashboardConfig.next(this.cfg);
            this.markForCheck();
        }
    }

    public async recordChange(description: string)
    {
        this.versionStateResolved = false;
        await this.versionManager.recordStateChange(description);
        this.updateWidgetLayout();
        this.versionStateResolved = true;
    }

    //--//

    public async collapseVertically()
    {
        if (this.widgetManipulator.collapseVertically())
        {
            this.widgetManipulator.refresh(false);
            await this.recordChange("collapse vertically");
            this.markForCheck();
        }
    }

    //--//

    public async saveEdits()
    {
        let baseVersionExt = this.versionManager.baseVersion;
        if (this.versionManager.drafting)
        {
            this.versionManager.clearCurrentDraft();

            let details = await this.versionManager.baseVersion.getDetails();
            if (!UtilsService.compareJson(this.m_cfg.model, details))
            {
                await this.m_cfg.saveWithNotifications(baseVersionExt, undefined, "Failed to save dashboard");

                await this.versionManager.updateBaseVersion(this.app.domain.dashboard.getHeadIdentity());
            }
        }
        else if (baseVersionExt && baseVersionExt.model.sysId !== this.app.domain.dashboard.getHeadIdentity().sysId)
        {
            await this.app.domain.dashboard.makeHead(baseVersionExt);
        }

        this.m_cfg.model.widgets = UtilsService.arrayCopy(this.m_cfg.model.widgets);

        this.app.domain.dashboard.disableEdit();
    }

    public async cancelEdits()
    {
        let active = await this.app.domain.dashboard.getActive();
        await this.restoreToState(active.cloneForEdit(), false, null);

        this.app.domain.dashboard.disableEdit();
    }

    //--//

    public async newWidget()
    {
        return this.updateWidget(null, null, null);
    }

    public async editWidget(editWidgetRequest: EditWidgetRequest)
    {
        let startingStep: string;
        if (editWidgetRequest.editConfig)
        {
            let configExt = WidgetConfigurationExtended.fromConfigModel(editWidgetRequest.editConfig.config);
            startingStep  = configExt.startingStep();
        }

        await this.updateWidget(editWidgetRequest.editConfig, editWidgetRequest.parentConfig, startingStep);
    }

    private async updateWidget(composition: Models.WidgetComposition,
                               parentCfg: Models.GroupingWidgetConfiguration,
                               startingStep: string)
    {
        let parentManipulator: WidgetManipulator;
        if (parentCfg)
        {
            const parentContainer = this.widgetManipulator.getWidgetContainer(parentCfg.id);
            parentManipulator     = WidgetManipulator.getInnerManipulator(parentContainer);
        }
        if (!parentManipulator) parentManipulator = this.widgetManipulator;
        const state = new WidgetEditorWizardState(this,
                                                  composition,
                                                  parentManipulator,
                                                  parentManipulator !== this.widgetManipulator,
                                                  startingStep);

        if (await WidgetEditorWizardDialogComponent.open(state))
        {
            const dashboardExt          = state.editor.dashboardExt;
            const cfg                   = state.editor.widget;
            const newSelectorNameLookup = state.editor.newSelectorNameLookup;

            const updatedCfg = parentCfg ?
                dashboardExt.updateSubWidget(parentCfg.id, cfg, newSelectorNameLookup) :
                dashboardExt.updateWidget(cfg, undefined, newSelectorNameLookup);

            if (updatedCfg)
            {
                const message = `${composition ? "edit" : "create new"} ${parentCfg ? "sub-widget" : "widget"}`;
                this.executeEdit(async () =>
                                 {
                                     this.cfg = updatedCfg;
                                     await this.recordChange(message);
                                     this.markForCheck();

                                     // If we need to move to the widget, schedule a scroll
                                     if (!composition) this.scrollToWidget(cfg.id);
                                 });
            }
        }
    }

    public async deleteWidgets(removalGraph: WidgetGraph)
    {
        await this.executeEdit(async () =>
                               {
                                   let nodesToDelete = removalGraph.findNodes((node) => node.marked);
                                   let idsToDelete   = new Set<string>(nodesToDelete.map((node) => node.widget.id));

                                   let deleted      = new Set<string>();
                                   let widgetLayout = this.m_cfg.model.widgets;
                                   for (let i = widgetLayout.length - 1; i >= 0; i--)
                                   {
                                       let widgetId = widgetLayout[i].config.id;
                                       if (idsToDelete.has(widgetId))
                                       {
                                           this.widgetManipulator.recordWidgetDeletion(widgetId);
                                           widgetLayout.splice(i, 1);
                                           deleted.add(widgetId);
                                       }
                                   }

                                   let parentNodes = new Set<WidgetNode>();
                                   for (let node of nodesToDelete)
                                   {
                                       if (!deleted.has(node.widget.id))
                                       {
                                           let parentNode = node.parent;
                                           if (!parentNodes.has(parentNode))
                                           {
                                               parentNodes.add(parentNode);
                                               parentNode.widget.deleteMarkedChildren(parentNode);
                                           }
                                       }
                                   }

                                   this.m_cfg.model.widgets = UtilsService.arrayCopy(this.m_cfg.model.widgets);

                                   await this.recordChange(`delete ${nodesToDelete.length} ${UtilsService.pluralize("widget", nodesToDelete.length)}`);
                               });
    }

    private async executeEdit(action: () => Promise<void>)
    {
        if (this.editing)
        {
            await action();
        }
        else
        {
            this.app.domain.dashboard.triggerEdit(action);
        }
    }

    public async clearWidgets()
    {
        if (this.m_cfg.model.widgets.length > 0)
        {
            let cfg                                  = this.m_cfg.cloneForEdit();
            cfg.model.widgets                        = [];
            cfg.model.sharedSelectors                = [];
            this.widgetManipulator.widgetOutlineExts = [];
            this.detectChanges();
            this.cfg = cfg;
            await this.recordChange("clear widgets");
        }
    }

    //--//

    private static widgetImportValidator(config: Models.WidgetConfiguration): string
    {
        if (config instanceof Models.TextWidgetConfiguration)
        {
            return "Text widgets are not allowed outside of grouping widgets";
        }

        return "";
    }

    public async importWidget()
    {
        let widgetInfo = await WidgetImportDialog.open(this, DashboardPageComponent.widgetImportValidator);
        if (widgetInfo != null)
        {
            let updatedCfg = this.m_cfg.addWidget(widgetInfo.config, widgetInfo.selectors, widgetInfo.graphs, widgetInfo.outline);
            if (updatedCfg)
            {
                this.cfg = updatedCfg;
                await this.recordChange("import widget");
                this.markForCheck();
                this.app.domain.dashboard.errors.success("Widget imported", -1);

                // Scroll to the imported widget
                this.scrollToWidget((<Models.WidgetConfiguration>widgetInfo.config).id);
            }
        }
    }

    public async pasteWidgets()
    {
        let pastedByType = new Map<any, ClipboardEntryData<any, any>[]>();
        let numAdded     = 0;
        let cfg          = this.m_cfg;
        let lastAdded: Models.WidgetConfiguration;
        for (let data of this.clipboardService.getAll())
        {
            let config = data.getDashboardWidget();
            if (config)
            {
                if (config instanceof Models.TextWidgetConfiguration) continue;

                let updatedCfg = cfg.addWidget(config, data.selectors, data.selectorGraphs, data.widgetOutline);
                if (updatedCfg)
                {
                    let prototype     = Object.getPrototypeOf(data);
                    let alreadyPasted = pastedByType.get(prototype);
                    if (!alreadyPasted)
                    {
                        alreadyPasted = [];
                        pastedByType.set(prototype, alreadyPasted);
                    }

                    alreadyPasted.push(data);
                    lastAdded = config;
                    numAdded++;

                    cfg = updatedCfg;
                }
            }
        }

        let pasteDescription;
        for (let pasted of pastedByType.values())
        {
            if (!pasted?.length) continue;

            if (!pasteDescription)
            {
                pasteDescription = `${pasted.length} ${pasted[0].description} ${UtilsService.pluralize("widget", pasted.length)}`;
            }
            else
            {
                pasteDescription = `${numAdded} ${UtilsService.pluralize("widget", numAdded)}`;
                break;
            }
        }

        if (pasteDescription)
        {
            this.updateCfg(cfg);
            this.markForCheck();
            await this.recordChange("paste " + pasteDescription);

            // Scroll to the last added widget if it exists
            if (lastAdded) this.scrollToWidget(lastAdded.id);
        }
    }

    //--//

    public async bannerToggled()
    {
        this.widgetManipulator.bannerToggled(this.m_cfg.model.showTitle);

        await this.recordChange(`${this.m_cfg.model.showTitle ? "enable" : "disable"} banner`);
    }

    private async scrollToWidget(id: string)
    {
        // Scroll to the specified widget after a small delay
        await Future.delayed(250);

        // Select and scroll to widget
        let widget = this.widgetManipulator.getWidgetContainer(id);
        if (widget) widget.element.nativeElement.scrollIntoView({behavior: "smooth"});
    }
}
