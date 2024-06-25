import {ChangeDetectionStrategy, Component, ViewChild} from "@angular/core";

import {DataExplorerPageComponent} from "app/customer/visualization/data-explorer-page.component";
import {InteractableSourcesChart, TimeSeriesChartConfigurationExtended} from "app/customer/visualization/time-series-utils";
import {WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {ClipboardEntryData} from "app/services/domain/clipboard.service";
import {AssetContextSubscriptionPayload} from "app/services/domain/dashboard-management.service";
import {WidgetConfigurationExtended, WidgetDef} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";
import {ChartSetComponent, ExternalGraphChart} from "app/shared/charting/chart-set.component";
import {TimeSeriesChartComponent} from "app/shared/charting/time-series-chart/time-series-chart.component";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {CanvasZoneSelection} from "framework/ui/charting/app-charting-utilities";
import {VerticalViewWindow} from "framework/ui/charting/vertical-view-window";
import {getSubjectValue, mapInParallel, someInParallel} from "framework/utils/concurrency";
import {BehaviorSubject} from "rxjs";

@Component({
               selector       : "o3-time-series-widget",
               templateUrl    : "./widget.template.html",
               styleUrls      : ["./widget.styles.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class TimeSeriesWidgetComponent extends WidgetBaseComponent<Models.TimeSeriesWidgetConfiguration, TimeSeriesWidgetConfigurationExtended>
{
    @ViewChild("visualization") set content(content: ChartSetComponent)
    {
        this.activeVisualization = content;
        this.activeVisualization?.refreshConsolidated();
        this.activeVisualizationSubject.next(this.activeVisualization);
        this.refresh();
    }

    activeVisualization: ChartSetComponent;
    activeVisualizationSubject                    = new BehaviorSubject<ChartSetComponent>(null);
    viewWindow: VerticalViewWindow;
    range: Models.RangeSelection;
    charts: Models.TimeSeriesChartConfiguration[] = [];
    externalGraphCharts: ExternalGraphChart[]     = [];
    externalGraphHost: GraphConfigurationHost;

    annotations: CanvasZoneSelection[]             = [];
    annotationMap: Map<CanvasZoneSelection, TimeSeriesChartComponent>;
    interactableCharts: InteractableSourcesChart[] = [];
    m_chartChipType: InteractableChartType;

    get hasData(): boolean
    {
        return this.range && !!this.externalGraphCharts?.length;
    }

    get isLine(): boolean
    {
        return this.m_chartChipType === InteractableChartType.Line;
    }

    get annotationOptionLabel(): string
    {
        let numAnnotations = this.annotations.length;
        return `${numAnnotations} ${UtilsService.pluralize("Annotation", numAnnotations)}`;
    }

    public async bind()
    {
        await super.bind();

        if (!this.config.name) this.config.name = "Time Series Chart(s)";

        this.range  = this.config.range;
        this.charts = this.config.charts || [];

        this.getSourceChipType();

        let hasSources = await someInParallel(this.externalGraphCharts, async (configWithExternal) =>
        {
            if (!configWithExternal) return false;

            let copyConfig             = Models.TimeSeriesChartConfiguration.newInstance(configWithExternal.config);
            let copyConfigWithExternal = new ExternalGraphChart(copyConfig, configWithExternal.externalGraph);
            let configExt              = await copyConfigWithExternal.getChartConfigExt(this.app);
            return configExt.chartHandler.hasSources();
        });
        if (hasSources) this.changeLoading(true);
    }

    private externalBindingsAllowed(config: Models.TimeSeriesChartConfiguration): boolean
    {
        switch (config.type)
        {
            case Models.TimeSeriesChartType.GRAPH:
            case Models.TimeSeriesChartType.STANDARD:
            case Models.TimeSeriesChartType.COORDINATE:
                return true;
        }

        return false;
    }

    protected async dashboardUpdated(): Promise<void>
    {
        await super.dashboardUpdated();

        this.externalGraphHost = this.dashboard.graphConfigurationHost;

        let subPayloads: AssetContextSubscriptionPayload[] = [];
        this.externalGraphCharts                           = await mapInParallel(this.config.charts, async (config,
                                                                                                            chartIdx) =>
        {
            let externalGraphConfig = Models.TimeSeriesGraphConfiguration.newInstance({
                                                                                          sharedGraphs: [],
                                                                                          contexts    : []
                                                                                      });
            if (this.externalBindingsAllowed(config))
            {
                let selectorIdToBinding: Lookup<Models.AssetGraphBinding> = {};
                for (let binding of config.graph?.externalBindings || []) selectorIdToBinding[binding.selectorId] = binding;

                for (let selectorId in selectorIdToBinding)
                {
                    let graphId = selectorIdToBinding[selectorId].graphId;

                    let currContextId: string;
                    subPayloads.push(new AssetContextSubscriptionPayload(selectorId, async (context) =>
                    {
                        if (context != null && context.sysId !== currContextId)
                        {
                            currContextId = context.sysId;

                            let graphExt = await this.dashboard.getResolvedGraph(graphId);
                            if (graphExt)
                            {
                                externalGraphConfig.sharedGraphs = externalGraphConfig.sharedGraphs.filter((sharedGraph) => sharedGraph.id !== graphId);
                                externalGraphConfig.sharedGraphs.push(graphExt.modelClone());

                                externalGraphConfig.contexts = externalGraphConfig.contexts.filter((context) => context.graphId !== graphId);
                                externalGraphConfig.contexts.push(Models.AssetGraphContextAsset.newInstance({
                                                                                                                ...context,
                                                                                                                graphId: graphId,
                                                                                                                nodeId : graphExt.getRootNodes()[0].id
                                                                                                            }));

                                await this.externalGraphCharts[chartIdx].updateConfigExt();
                                this.externalGraphCharts = UtilsService.arrayCopy(this.externalGraphCharts);
                                this.markForCheck();
                            }
                        }
                    }));
                }
            }

            return new ExternalGraphChart(config, externalGraphConfig);
        });

        await this.registerContextSubscriptions(...subPayloads);
    }

    public async viewInDataExplorer()
    {
        let viz     = await getSubjectValue(this.activeVisualizationSubject);
        let configs = await getSubjectValue(viz.configExtsSubject);

        let configExts = configs.map((config) =>
                                     {
                                         let configExt = TimeSeriesChartConfigurationExtended.constructFrom(config);
                                         if (configExt.model.type === Models.TimeSeriesChartType.COORDINATE && configExt.hasExternalBindings)
                                         {
                                             configExt.model.graph.externalBindings = [];
                                         }
                                         else
                                         {
                                             configExt.adoptExternalGraphs();
                                         }
                                         return configExt;
                                     });

        let ui = this.app.ui;
        await DataExplorerPageComponent.visualizeCharts(ui.navigation, ui.viewstate, configExts, this.range);
    }

    public async refreshSize(): Promise<boolean>
    {
        let loaded = !!this.activeVisualization?.refreshHeight(this.heightRaw);

        if (loaded && this.charts?.length)
        {
            this.detectChanges();

            this.activeVisualization.refreshZoomability();
        }

        if (loaded) this.updateMenuItems();

        this.markForCheck();

        return loaded;
    }

    public onMouseMove(event: MouseEvent,
                       scrollTop: number)
    {
        super.onMouseMove(event, scrollTop);

        this.activeVisualization?.onMouseMove(event, scrollTop);
    }

    public onMouseLeave()
    {
        super.onMouseLeave();

        this.activeVisualization?.onMouseLeave();
    }

    /**
     * don't set anything if they're not all alike
     */
    private getSourceChipType()
    {
        let chartTypes = this.charts.map((chart) =>
                                         {
                                             switch (chart.type)
                                             {
                                                 case Models.TimeSeriesChartType.STANDARD:
                                                 case Models.TimeSeriesChartType.GRAPH:
                                                     return InteractableChartType.Line;

                                                 case Models.TimeSeriesChartType.SCATTER:
                                                 case Models.TimeSeriesChartType.GRAPH_SCATTER:
                                                     return InteractableChartType.Scatter;

                                                 case Models.TimeSeriesChartType.COORDINATE:
                                                     return InteractableChartType.GPS;

                                                 default:
                                                     return null;
                                             }
                                         });

        let firstType = chartTypes[0];
        if (firstType && chartTypes.every((type) => type === firstType)) this.m_chartChipType = firstType;
    }

    public updateMenuItems()
    {
        if (this.m_chartChipType)
        {
            let interactableCharts = [];
            let annotations        = [];
            let annotationMap      = new Map<CanvasZoneSelection, TimeSeriesChartComponent>();
            for (let container of this.activeVisualization?.set?.timeSeriesContainers || [])
            {
                interactableCharts.push(container.interactableChart);

                if (container.isLine)
                {
                    let timeSeriesChart = <TimeSeriesChartComponent>container.interactableChart;
                    for (let annotation of timeSeriesChart?.annotations || [])
                    {
                        annotations.push(annotation);
                        annotationMap.set(annotation, timeSeriesChart);
                    }
                }
            }

            if (!UtilsService.compareArraysAsSets(this.interactableCharts, interactableCharts))
            {
                this.interactableCharts = interactableCharts;
            }

            if (!UtilsService.compareArraysAsSets(this.annotations, annotations))
            {
                this.annotations   = annotations;
                this.annotationMap = annotationMap;
            }

            this.markForCheck();
        }
    }

    protected dimensionsUpdated()
    {
        this.updateViewWindow();
        this.activeVisualization?.refreshConsolidated();

        super.dimensionsUpdated();
    }

    protected focusUpdated()
    {
        super.focusUpdated();

        this.updateLayout();
    }

    protected editingUpdated()
    {
        super.editingUpdated();

        this.updateLayout();
    }

    private updateLayout()
    {
        this.markForCheck();

        // let browser redraw so that the canvas gets its correct size
        setTimeout(() => this.refreshSize());
    }

    protected scrollTopUpdated()
    {
        this.updateViewWindow();
    }

    private updateViewWindow()
    {
        this.viewWindow = new VerticalViewWindow(this.scrollTop || 0, this.heightRaw);
        this.markForCheck();
    }

    protected getClipboardData(): ClipboardEntryData<Models.TimeSeriesWidgetConfiguration, Models.ReportLayoutItem>
    {
        let model = Models.TimeSeriesWidgetConfiguration.deepClone(this.config);

        return new class extends ClipboardEntryData<Models.TimeSeriesWidgetConfiguration, Models.ReportLayoutItem>
        {
            constructor()
            {
                super("chart");
            }

            public getDashboardWidget(): Models.TimeSeriesWidgetConfiguration
            {
                return Models.TimeSeriesWidgetConfiguration.deepClone(model);
            }

            public getReportItem(): Models.ReportLayoutItem
            {
                let element = Models.CustomReportElementChartSet.newInstance({
                                                                                 charts: model.charts
                                                                             });
                return Models.ReportLayoutItem.newInstance({element: element});
            }
        }();
    }
}

enum InteractableChartType
{
    Line    = "Line",
    Scatter = "Scatter",
    GPS     = "GPS"
}

@WidgetDef({
               friendlyName      : "Chart",
               typeName          : "TIME_SERIES",
               model             : Models.TimeSeriesWidgetConfiguration,
               component         : TimeSeriesWidgetComponent,
               classes           : ["scrollable"],
               dashboardCreatable: true,
               subgroupCreatable : true,
               maximizable       : true,
               defaultWidth      : 6,
               defaultHeight     : 7,
               hostScalableText  : false,
               needsProtector    : true,
               documentation     : {
                   description: "The Chart widget is a powerful tool that allows you to display control point data in a variety of highly configurable ways: As a line or area chart, a scatter plot, or as compact hierarchical heatmaps and line charts.",
                   examples   : [
                       {
                           file       : "widgets/TIME_SERIES/timeseries.png",
                           label      : "Multi Panel Chart",
                           description: "Standard line chart with multiple control points shown across multiple panels, displayed as a 12x11 widget."
                       },
                       {
                           file       : "widgets/TIME_SERIES/scatter.png",
                           label      : "Scatter Plot",
                           description: "Scatter plot where one asset has two control points plotted against each other, displayed as a 6x6 widget."
                       },
                       {
                           file       : "widgets/TIME_SERIES/scatter_gradient.png",
                           label      : "Scatter Plot With Gradient",
                           description: "Scatter plot where one asset has three control points plotted against each other, one on the x axis, one on the y axis, and a third as a color gradient. Displayed as a 6x6 widget."
                       },
                       {
                           file       : "widgets/TIME_SERIES/hierarchical_line.png",
                           label      : "hierarchical Line Chart",
                           description: "Hierarchical control point query with the data visualized as compact line charts, displayed as a 12x7 widget."
                       },
                       {
                           file       : "widgets/TIME_SERIES/hierarchical_heatmap.png",
                           label      : "hierarchical Heatmap Chart",
                           description: "Hierarchical control point query with the data visualized as compact heatmaps, displayed as a 12x7 widget."
                       }
                   ]
               }

           })
export class TimeSeriesWidgetConfigurationExtended extends WidgetConfigurationExtended<Models.TimeSeriesWidgetConfiguration>
{
    protected initializeForWizardInner()
    {
    }

    public getBindings(): Models.AssetGraphBinding[]
    {
        let bindings: Models.AssetGraphBinding[] = [];
        for (let chart of this.model.charts || [])
        {
            for (let binding of chart.graph?.externalBindings || [])
            {
                bindings.push(Models.AssetGraphBinding.newInstance(binding));
            }
        }
        return bindings;
    }
}
