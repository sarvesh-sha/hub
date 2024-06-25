import {ChangeDetectionStrategy, Component, ContentChildren, EventEmitter, Input, Output, QueryList, ViewChild} from "@angular/core";
import {AppContext} from "app/app.service";

import {TimeSeriesChartConfigurationExtended} from "app/customer/visualization/time-series-utils";
import * as SharedSvc from "app/services/domain/base.service";
import {GraphContextUpdater} from "app/services/domain/dashboard-management.service";
import * as Models from "app/services/proxy/model/models";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";
import {TimeSeriesSetToolbarActionComponent, TimeSeriesSetToolbarComponent} from "app/shared/charting/time-series-set/time-series-set-toolbar.component";
import {TimeSeriesSetComponent} from "app/shared/charting/time-series-set/time-series-set.component";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";
import {VerticalViewWindow} from "framework/ui/charting/vertical-view-window";
import {mapInParallel} from "framework/utils/concurrency";
import {AsyncDebouncer} from "framework/utils/debouncers";
import {BehaviorSubject} from "rxjs";


@Component({
               selector       : "o3-chart-set[configs], o3-chart-set[externalGraphCharts]",
               templateUrl    : "./chart-set.component.html",
               styleUrls      : ["./chart-set.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ChartSetComponent extends SharedSvc.BaseApplicationComponent
{
    private static readonly maxViewWindow = new VerticalViewWindow(0, Number.MAX_VALUE);

    private m_withExternalGraphs: boolean;

    private m_rebuildDebouncer = new AsyncDebouncer<void>(10, async () =>
    {
        if (!this.m_withExternalGraphs && this.m_configs?.length)
        {
            this.configExts = await mapInParallel(this.m_configs, (config) => TimeSeriesChartConfigurationExtended.newInstance(this.app, config));
        }
        else if (this.m_withExternalGraphs && this.m_externalGraphCharts?.length)
        {
            this.configExts = await mapInParallel(this.m_externalGraphCharts,
                                                  (configWithExternal) => configWithExternal.getChartConfigExt(this.app));
        }

        this.configExtsSubject.next(this.configExts);

        this.markForCheck();
    });

    private m_configs: Models.TimeSeriesChartConfiguration[] = [];
    @Input() set configs(configs: Models.TimeSeriesChartConfiguration[])
    {
        this.m_configs            = configs;
        this.m_withExternalGraphs = false;
        this.m_rebuildDebouncer.invoke();
    }

    private m_externalGraphCharts: ExternalGraphChart[] = [];
    @Input() set externalGraphCharts(configs: ExternalGraphChart[])
    {
        this.m_configs = configs?.map((configWithExternal) => configWithExternal.config);

        this.m_externalGraphCharts = configs;
        this.m_withExternalGraphs  = true;
        this.m_rebuildDebouncer.invoke();
    }

    @Input() range: Models.RangeSelection = RangeSelectionExtended.newModel();
    @Input() externalGraphsHost: GraphConfigurationHost;

    @Input() readonly                 = false;
    @Input() canDeleteAllCharts       = false;
    @Input() canDeleteAllSources      = true;
    @Input() resizable                = true;
    @Input() zoomable                 = true;
    @Input() handleInteractions       = true;
    @Input() allowConsolidatedSources = true;
    @Input() embedded                 = false;
    @Input() renderAll                = false;
    @Input() allowDashboardAdd        = true;
    @Input() printable                = false;
    @Input() staticRange              = false;
    @Input() staticRangeTooltip: string;

    private m_viewWindow: VerticalViewWindow;
    @Input() set viewWindow(viewWindow: VerticalViewWindow)
    {
        this.m_viewWindow = viewWindow;
    }

    get viewWindow(): VerticalViewWindow
    {
        return this.renderAll ? ChartSetComponent.maxViewWindow : this.m_viewWindow;
    }

    @Output() configsChange              = new EventEmitter<Models.TimeSeriesChartConfiguration[]>();
    @Output() externalGraphChartsChange  = new EventEmitter<ExternalGraphChart[]>();
    @Output() rangeChange                = new EventEmitter<Models.RangeSelection>();
    @Output() chartUpdated               = new EventEmitter<boolean>();
    @Output() sourcesConsolidatedUpdated = new EventEmitter<void>();
    @Output() startedFetchingData        = new EventEmitter<void>();
    @Output() stoppedFetchingData        = new EventEmitter<void>();

    configExts: TimeSeriesChartConfigurationExtended[] = [];
    configExtsSubject                                  = new BehaviorSubject<TimeSeriesChartConfigurationExtended[]>(null);
    actions: TimeSeriesSetToolbarActionComponent[]     = [];

    @ViewChild(TimeSeriesSetComponent, {static: true}) set: TimeSeriesSetComponent;
    @ViewChild("toolbar") toolbar: TimeSeriesSetToolbarComponent;

    @ContentChildren(TimeSeriesSetToolbarActionComponent)
    set actionTemplates(actionTemplates: QueryList<TimeSeriesSetToolbarActionComponent>)
    {
        this.actions = actionTemplates.toArray();
    }

    get hasCharts(): boolean
    {
        return !!this.m_configs?.length;
    }

    refreshConsolidated()
    {
        this.set?.refreshConsolidated();
    }

    refreshHeight(height: number,
                  retries: number = 0): boolean
    {
        let toolbarHeight = !this.readonly && this.toolbar?.elementRef?.nativeElement.offsetHeight || 0;
        return this.set.refreshHeight(height - toolbarHeight, retries);
    }

    refreshZoomability()
    {
        this.set.refreshZoomability();
    }

    chartsChanged(charts: TimeSeriesChartConfigurationExtended[])
    {
        if (charts.length == 0)
        {
            this.configExts = charts;
            this.configExtsSubject.next(charts);
            this.m_configs = [];
            if (this.m_withExternalGraphs) this.m_externalGraphCharts = [];
        }
        else if ((!this.m_withExternalGraphs && charts.length > this.m_configs.length) ||
                 (this.m_withExternalGraphs && charts.length > this.m_externalGraphCharts.length))
        {
            // chartsChanged triggered by TimeSeriesSetToolbar: change detection not automatically taking place in TimeSeriesSet
            let configExt = charts[charts.length - 1];
            this.m_configs.push(configExt.model);
            if (this.m_withExternalGraphs) this.m_externalGraphCharts.push(ExternalGraphChart.fromChartExt(configExt));

            this.set.chartAdded();
        }
        else
        {
            // chart edited or deleted
            let changedIdx = this.m_configs.findIndex((chart,
                                                       index) => charts[index]?.model !== chart);

            if (charts.length < this.m_configs.length)
            {
                this.m_configs.splice(changedIdx, 1);
                if (this.m_withExternalGraphs) this.m_externalGraphCharts.splice(changedIdx, 1);
            }
            else
            {
                let configExt              = charts[changedIdx];
                this.m_configs[changedIdx] = configExt.model;
                if (this.m_withExternalGraphs) this.m_externalGraphCharts[changedIdx].configExtUpdated(configExt);
            }
        }

        this.configsChange.emit(this.m_configs);
        if (this.m_withExternalGraphs) this.externalGraphChartsChange.emit(this.m_externalGraphCharts);
    }

    onMouseMove(event: MouseEvent,
                scrollTop: number)
    {
        this.set.onMouseMove(event, scrollTop);
    }

    onMouseLeave()
    {
        this.set.onMouseLeave();
    }
}

export class ExternalGraphChart
{
    private m_configExt: TimeSeriesChartConfigurationExtended;

    get externalGraph(): Models.TimeSeriesGraphConfiguration
    {
        return this.m_externalGraph;
    }

    set externalGraph(graph: Models.TimeSeriesGraphConfiguration)
    {
        this.m_externalGraph = graph;
        if (this.m_configExt)
        {
            this.m_configExt.updateExternalGraph(this.m_externalGraph);
        }
    }

    set contextUpdaters(contextUpdaters: GraphContextUpdater[])
    {
        this.m_externalContextUpdaters = contextUpdaters;
        if (this.m_configExt)
        {
            this.m_configExt.externalContextUpdaters = contextUpdaters;
        }
    }

    constructor(public config: Models.TimeSeriesChartConfiguration,
                private m_externalGraph: Models.TimeSeriesGraphConfiguration,
                private m_externalContextUpdaters?: GraphContextUpdater[])
    {}

    async getChartConfigExt(app: AppContext): Promise<TimeSeriesChartConfigurationExtended>
    {
        if (!this.m_configExt && app)
        {
            this.m_configExt                         = await TimeSeriesChartConfigurationExtended.newInstance(app, this.config, undefined, this.m_externalGraph);
            this.m_configExt.externalContextUpdaters = this.m_externalContextUpdaters;
        }

        return this.m_configExt;
    }

    async updateConfigExt()
    {
        if (this.m_configExt)
        {
            await this.m_configExt.applyStandardGraphSourceChanges(this.m_configExt.standardGraphBindingSet());
        }
    }

    configExtUpdated(configExt: TimeSeriesChartConfigurationExtended)
    {
        this.m_configExt = configExt;
        this.config      = configExt.model;
    }

    public static fromChartExt(configExt: TimeSeriesChartConfigurationExtended): ExternalGraphChart
    {
        let configWithExternal         = new ExternalGraphChart(configExt.model, configExt.externalGraph, configExt.externalContextUpdaters);
        configWithExternal.m_configExt = configExt;

        return configWithExternal;
    }
}
