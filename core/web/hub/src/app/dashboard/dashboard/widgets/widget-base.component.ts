import {Directive, ElementRef, EventEmitter, HostListener, Inject, InjectionToken, Injector, Output, QueryList, ViewChildren} from "@angular/core";

import {WidgetContainerComponent} from "app/dashboard/dashboard/widgets/widget-container.component";
import {EditWidgetRequest} from "app/dashboard/dashboard/widgets/widget-manager.component";
import {WidgetManipulator, WidgetOutlineExtended} from "app/dashboard/dashboard/widgets/widget-manipulator";
import * as SharedSvc from "app/services/domain/base.service";
import {ClipboardEntryData, ClipboardService} from "app/services/domain/clipboard.service";
import {AssetContextSubscriptionPayload, DashboardConfigurationExtended, WidgetGraph, WidgetNode} from "app/services/domain/dashboard-management.service";
import {WidgetConfigurationExtended} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {AssetGraphContextAsset} from "app/services/proxy/model/models";
import {Lookup} from "framework/services/utils.service";

import {ControlOption} from "framework/ui/control-option";
import {TabActionDirective} from "framework/ui/shared/tab-action.directive";
import {Future, inParallel} from "framework/utils/concurrency";
import {getHotkeyAction, HotkeyAction} from "framework/utils/keyboard-hotkeys";

import {Subscription} from "rxjs";

export const CONTAINER_DATA = new InjectionToken<{}>("CONTAINER_DATA");
export const CONTAINER_PTR  = new InjectionToken<{}>("CONTAINER_PTR");

export interface WidgetData<TConfiguration extends Models.WidgetConfiguration, TExtended extends WidgetConfigurationExtended<TConfiguration>>
{
    configExt: TExtended;
}

@Directive()
export abstract class WidgetBaseComponent<TConfiguration extends Models.WidgetConfiguration, TExtended extends WidgetConfigurationExtended<TConfiguration>> extends SharedSvc.BaseApplicationComponent
{
    get id(): string
    {
        return this.config.id;
    }

    get config(): TConfiguration
    {
        return this.m_configExt.model;
    }

    private readonly m_configExt: TExtended;
    get configExt(): TExtended
    {
        return this.m_configExt;
    }

    private readonly m_selectorBindings: Models.AssetGraphBinding[] = [];
    get selectorBindings(): Models.AssetGraphBinding[]
    {
        return this.m_selectorBindings;
    }

    private m_contextSubscriptions: Lookup<Subscription>;
    private m_optionSubscription: Subscription;
    private m_dashboard: DashboardConfigurationExtended;
    protected get dashboard(): DashboardConfigurationExtended
    {
        return this.m_dashboard;
    }

    private m_width: number;
    private m_height: number;

    get widthRaw(): number
    {
        return this.m_width || 0;
    }

    get heightRaw(): number
    {
        return this.m_height || 0;
    }

    private m_scrollTop: number;
    get scrollTop(): number
    {
        return this.m_scrollTop;
    }

    set scrollTop(scrollTop: number)
    {
        if (this.m_scrollTop != scrollTop)
        {
            this.m_scrollTop = scrollTop;
            this.scrollTopUpdated();
        }
    }

    protected scrollTopUpdated(): void
    {
    }

    private m_baseFont: number;
    set baseFont(baseFont: number)
    {
        this.m_baseFont = baseFont;
        if (this.config.manualFontScaling) this.fontSizeUpdated();
    }

    protected fontSizeUpdated(): void
    {
    }

    get manualFontSize(): number
    {
        if (this.config.manualFontScaling && this.config.fontMultiplier && this.m_baseFont)
        {
            return this.config.fontMultiplier * this.m_baseFont;
        }

        return undefined;
    }

    @ViewChildren(TabActionDirective) set customOptions(options: QueryList<TabActionDirective>)
    {
        if (options) this.customMenuOptions = options.toArray();
    }

    customMenuOptions: TabActionDirective[];

    private m_bound   = false;
    private m_loading = false;

    private m_isLoading = true;
    get isLoading(): boolean
    {
        return this.m_isLoading;
    }

    inView = false;
    loaded = true;

    relatedToFocus = false;

    private m_focus = false;
    get focus(): boolean
    {
        return this.m_focus;
    }

    set focus(focus: boolean)
    {
        this.m_focus = focus;
        this.focusUpdated();
    }

    protected focusUpdated(): void
    {
    }

    private m_editing = false;
    set editing(editing: boolean)
    {
        this.m_editing = editing;
        this.editingUpdated();
    }

    get editing(): boolean
    {
        return this.m_editing;
    }

    protected editingUpdated(): void
    {
    }

    get removable(): boolean
    {
        let widgetManipulator = this.dashboard.widgetManipulator;
        if (widgetManipulator?.synchronized)
        {
            let graph = new WidgetGraph(widgetManipulator);
            if (!graph.getNode(this)) return false;

            this.mark(graph);
            return graph.canRemoveMarked();
        }

        return false;
    }

    get showToolbarTitle(): boolean
    {
        return true;
    }

    protected needsContentRefresh = false;

    private refreshRate: number;
    private contentRefresher: number;

    @HostListener("document:keydown", ["$event"]) onKeyDown(event: KeyboardEvent)
    {
        if (!this.app.domain.dashboard.widgetWizardOpen && this.m_focus)
        {
            this.handleHotkey(getHotkeyAction(event));
        }
    }

    @Output() loadingChanged    = new EventEmitter<boolean>();
    @Output() dimensionsChanged = new EventEmitter<void>();
    @Output() contentUpdated    = new EventEmitter<void>();
    @Output() widgetEdit        = new EventEmitter<EditWidgetRequest>();
    @Output() widgetDelete      = new EventEmitter<WidgetGraph>();
    @Output() cursorClassChange = new EventEmitter<string[]>();

    constructor(inj: Injector,
                public readonly element: ElementRef,
                protected clipboard: ClipboardService,
                @Inject(CONTAINER_DATA) private data: WidgetData<TConfiguration, TExtended>,
                @Inject(CONTAINER_PTR) public readonly container: WidgetContainerComponent)
    {
        super(inj);

        this.m_configExt        = this.data.configExt;
        this.m_selectorBindings = this.m_configExt.getBindings();
        this.m_dashboard        = this.app.domain.dashboard.currentDashboardConfig.getValue();
    }

    // return true if successfully refreshed size
    public abstract refreshSize(): Promise<boolean>;

    public isContextPublisher(): boolean
    {
        return !!this.m_optionSubscription;
    }

    public async load()
    {
        try
        {
            await this.bind();
        }
        finally
        {
            // don't appear to be loading even if something when wrong
            this.m_bound = true;
            this.updateIsLoading();
        }

        this.detectChanges();
    }

    public async executeWithLoading(callback: () => Promise<void>)
    {
        try
        {
            while (this.m_loading)
            {
                await Future.delayed(100);
            }

            this.changeLoading(true);
            await callback();
        }
        finally
        {
            this.changeLoading(false);
        }
    }

    public changeLoading(loading: boolean)
    {
        this.m_loading = loading;
        this.updateIsLoading();
    }

    private updateIsLoading()
    {
        let isLoading = !this.m_bound || this.heightRaw <= 0 || this.widthRaw <= 0 || this.m_loading;
        if (this.m_isLoading !== isLoading)
        {
            this.m_isLoading = isLoading;
            this.loadingChanged.emit(isLoading);
            this.markForCheck();
        }
    }

    public ngOnDestroy()
    {
        this.clearContentRefresher();

        super.ngOnDestroy();
    }

    private clearContentRefresher()
    {
        if (this.contentRefresher)
        {
            clearTimeout(this.contentRefresher);
            this.contentRefresher = undefined;
        }
    }

    private setUpContentRefresher()
    {
        if (this.refreshRate > 0 && !this.contentRefresher)
        {
            this.contentRefresher = setTimeout(() =>
                                               {
                                                   this.contentRefresher    = null;
                                                   this.needsContentRefresh = true;
                                                   if (this.loaded && this.inView) this.refreshContent();
                                               }, this.refreshRate);

        }
    }

    public async bind(): Promise<void>
    {
        let refreshRate = (this.config.refreshRateInSeconds ?? 0) * 1000;
        if (this.refreshRate != refreshRate)
        {
            this.refreshRate = refreshRate;

            this.clearContentRefresher();
            this.setUpContentRefresher();
        }

        this.subscribeToObservable(this.app.domain.dashboard.currentDashboardConfig, (dashboard) => this.updateDashboard(dashboard));

        await Promise.race([
                               Future.delayed(5000),
                               this.updateDashboard(this.app.domain.dashboard.currentDashboardConfig.getValue())
                           ]);
    }

    private async updateDashboard(dashboardExt: DashboardConfigurationExtended)
    {
        if (dashboardExt)
        {
            this.m_dashboard = dashboardExt;
            await this.dashboardUpdated();
        }
    }

    protected async dashboardUpdated(): Promise<void>
    {
    }

    protected async registerOptionSubscription(graphId: string,
                                               fn: (options: ControlOption<string>[]) => void)
    {
        if (this.m_optionSubscription)
        {
            this.m_optionSubscription.unsubscribe();
            this.m_optionSubscription = null;
        }

        let optionsObservable = await this.m_dashboard.getGraphOptionsObservable(graphId);
        if (optionsObservable)
        {
            this.m_optionSubscription = this.subscribeToObservable(optionsObservable, fn);
        }
    }

    protected async registerContextSubscriptions(...subscriptionPayloads: AssetContextSubscriptionPayload[])
    {
        let oldContextSubscriptions = this.m_contextSubscriptions;
        this.m_contextSubscriptions = {};

        for (let selectorId in oldContextSubscriptions)
        {
            oldContextSubscriptions[selectorId].unsubscribe();
        }

        await inParallel(subscriptionPayloads, async (payload) =>
        {
            let contextObservable = await this.m_dashboard.getGraphContextObservable(payload.selectorId);
            if (contextObservable)
            {
                let subscribed = new Future();
                subscribed.setResolveTimeout(null, 2000);
                this.m_contextSubscriptions[payload.selectorId] = this.subscribeToObservable(contextObservable, async (context: AssetGraphContextAsset) =>
                {
                    await this.executeWithLoading(() => payload.fn(context));

                    subscribed.resolve();
                });
                await subscribed;
            }
        });
    }

    protected handleHotkey(action: HotkeyAction)
    {
        switch (action)
        {
            case HotkeyAction.Copy:
                this.copy();
                break;

            case HotkeyAction.Cut:
                if (this.removable)
                {
                    this.copy();
                    this.remove();
                }
                break;

            case HotkeyAction.Delete:
                if (this.removable)
                {
                    this.remove();
                }
                break;
        }
    }

    public async refresh()
    {
        this.loaded = await this.refreshSize();
        if (this.loaded && this.inView && this.needsContentRefresh) this.refreshContent();
    }

    public async refreshContent()
    {
        this.needsContentRefresh = false;
        this.setUpContentRefresher();
    }

    public setDimensions(width: number,
                         height: number)
    {
        if (width !== this.m_width || this.m_height !== height)
        {
            this.m_width  = width;
            this.m_height = height;
            this.updateIsLoading();
            this.dimensionsUpdated();
        }
    }

    test_mouseMove: MouseEvent;

    public onMouseMove(event: MouseEvent,
                       scrollTop: number)
    {
        this.test_mouseMove = event;
    }

    public onMouseLeave()
    {
    }

    public getDesiredSize(): number
    {
        return null;
    }

    protected computeWidth(columnar: boolean,
                           padding: number,
                           overrideColWidth: number): number
    {
        if (isNaN(padding ?? NaN)) padding = this.app.css.DashboardWidgetPadding.asNumber;
        return this.container.outline?.computeWidthPx(columnar, overrideColWidth) - padding * 2;
    }

    public requiresColumnarView(padding: number,
                                overrideColWidth: number): boolean
    {
        return this.computeWidth(false, padding, overrideColWidth) < WidgetManipulator.MIN_WIDGET_TOOLBAR_WIDTH;
    }

    public getUpdatedColumnarHeight(): number
    {
        return WidgetOutlineExtended.BASE_COLUMNAR_HEIGHT;
    }

    protected dimensionsUpdated(): void
    {
        this.dimensionsChanged.emit();
        this.detectChanges();
    }

    public cannotRemoveTooltip(): string
    {
        return "";
    }

    public remove()
    {
        let widgetGraph = new WidgetGraph(this.dashboard.widgetManipulator);
        this.mark(widgetGraph);
        if (widgetGraph.canRemoveMarked())
        {
            this.widgetDelete.emit(widgetGraph);
        }
    }

    //--//

    public isAssetGraphRelated(widget: WidgetBaseComponent<any, any>): boolean
    {
        let selectorIds = new Set<string>();
        for (let binding of this.selectorBindings) selectorIds.add(binding.selectorId);

        return widget.selectorBindings.some((binding) => selectorIds.has(binding.selectorId) && !binding.nodeId);
    }

    // only push this widget's WidgetManipulator
    public collectWidgetManipulators(manipulators: WidgetManipulator[])
    {
    }

    public deleteMarkedChildren(node: WidgetNode)
    {
    }

    public mark(graph: WidgetGraph)
    {
        if (graph.markNode(this))
        {
            this.widgetMarked(graph);
        }
    }

    protected widgetMarked(graph: WidgetGraph)
    {
    }

    public canRemove(graph: WidgetGraph): boolean
    {
        return !this.container.readonly;
    }

    //--//

    protected abstract getClipboardData(): ClipboardEntryData<TConfiguration, any>;

    public async generateClipboardData(): Promise<ClipboardEntryData<TConfiguration, any>>
    {
        let data           = this.getClipboardData();
        let widgetOutline  = this.dashboard.widgetManipulator.getWidgetContainer(this.id).outline.model;
        data.widgetOutline = Models.WidgetOutline.deepClone(widgetOutline);

        await inParallel(this.selectorBindings, async (binding) =>
        {
            let graphId    = this.dashboard.getAssociatedGraphId(binding.selectorId);
            let graphExt   = await this.dashboard.getResolvedGraph(graphId);
            let graphModel = graphExt?.modelClone();
            if (graphModel)
            {
                data.selectorGraphs.push(graphModel);
            }
        });

        return data;
    }

    public async copy()
    {
        this.clipboard.copy(await this.generateClipboardData());
    }
}
