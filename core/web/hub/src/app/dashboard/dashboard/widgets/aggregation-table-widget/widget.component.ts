import {ChangeDetectionStrategy, Component, ViewChild} from "@angular/core";
import {UUID} from "angular2-uuid";

import {DeviceElementsDetailPageComponent} from "app/customer/device-elements/device-elements-detail-page.component";
import {DataExplorerPageComponent} from "app/customer/visualization/data-explorer-page.component";
import {InteractableSourcesChart, TimeSeriesChartConfigurationExtended} from "app/customer/visualization/time-series-utils";
import {ContextPaneComponent} from "app/dashboard/context-pane/panes/context-pane.component";
import {WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {AssetGraphExtended, AssetGraphResponseHolder, SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import {ClipboardEntryData} from "app/services/domain/clipboard.service";
import {ControlPointsGroupExtended, WidgetConfigurationExtended, WidgetDef} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {AggregationGroupDownloadGenerator, AggregationGroupsComponent, AggregationGroupsTimeRange} from "app/shared/aggregation/aggregation-groups.component";
import {DataAggregationComponent, DataAggregationType} from "app/shared/aggregation/data-aggregation.component";
import {ConsolidatedSourceChipComponent} from "app/shared/charting/source-chip/consolidated-source-chip.component";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";
import {Lookup, UtilsService} from "framework/services/utils.service";

import {Vector2} from "framework/ui/charting/charting-math";
import {ControlOption} from "framework/ui/control-option";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {debounceTimeAfterFirst} from "framework/utils/debouncers";

import {Subject} from "rxjs";

@Component({
               selector       : "o3-aggregation-table-widget",
               templateUrl    : "./widget.template.html",
               styleUrls      : ["./widget.styles.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AggregationTableWidgetComponent extends WidgetBaseComponent<Models.AggregationTableWidgetConfiguration, AggregationTableWidgetConfigurationExtended>
{
    overrideMode: Models.HierarchicalVisualizationType;
    modeOptions: ControlOption<Models.HierarchicalVisualizationType>[];

    selectedRange: number;

    paneConfig: Models.PaneConfiguration;
    paneGraphResponse: AssetGraphResponseHolder;
    paneOverlayConfig: OverlayConfig;
    paneModels: Models.Pane[];
    paneRange: Models.RangeSelection;

    interactableCharts: InteractableSourcesChart[] = [];

    private viewportUpdateDebouncer = new Subject<void>();
    viewport: Vector2;

    get activeMode(): Models.HierarchicalVisualizationType
    {
        return this.overrideMode || this.config.visualizationMode || Models.HierarchicalVisualizationType.TABLE;
    }

    get bindingTableViewAsLabel(): string
    {
        const labelPrefix = "View as ";
        return this.activeMode === Models.HierarchicalVisualizationType.TABLE_WITH_BAR ? labelPrefix + "table" : labelPrefix + "bar table";
    }

    get aggregationGroups(): AggregationGroupsComponent
    {
        return this.dataAggregation.aggregationGroups;
    }

    @ViewChild(OverlayComponent) private paneOverlay: OverlayComponent;
    @ViewChild(DataAggregationComponent, {static: true}) dataAggregation: DataAggregationComponent;
    @ViewChild(ConsolidatedSourceChipComponent) sourceChip: ConsolidatedSourceChipComponent;

    public async bind(): Promise<void>
    {
        await super.bind();

        if (!this.config.name) this.config.name = "Data Aggregation";

        let clickBehavior = this.config.clickBehavior;
        let paneConfigId  = clickBehavior?.type === Models.InteractionBehaviorType.Pane && clickBehavior.paneConfigId;
        if (paneConfigId)
        {
            try
            {
                this.paneConfig        = await this.app.domain.panes.getConfig(paneConfigId);
                this.paneGraphResponse = await new AssetGraphExtended(this.app.domain, this.paneConfig.graph).resolve();
                this.paneOverlayConfig = ContextPaneComponent.getOverlayConfig(this.app.ui.overlay);
            }
            catch (err)
            {
                console.error("Failed to load pane configuration");
            }
        }

        if (this.config.groups.length)
        {
            let tableOnly    = this.config.groups.some((group) => group.groupAggregationType === Models.AggregationTypeId.NONE);
            this.modeOptions = this.app.domain.widgetData.getVisualizationModeOptions(tableOnly);
        }
        await this.dataAggregation.bind();

        // Update viewport immediately and listen for updates
        this.subscribeToObservable(this.viewportUpdateDebouncer.pipe(debounceTimeAfterFirst(750)), () => this.updateViewport());
    }

    toggleBindingTableMode()
    {
        if (this.activeMode === Models.HierarchicalVisualizationType.TABLE_WITH_BAR)
        {
            this.overrideMode = Models.HierarchicalVisualizationType.TABLE;
        }
        else
        {
            this.overrideMode = Models.HierarchicalVisualizationType.TABLE_WITH_BAR;
        }
    }

    protected focusUpdated(): void
    {
        super.focusUpdated();

        this.markForCheck();
    }

    public async handleSelection(significantElement: DeviceElementExtended,
                                 range: Models.RangeSelection,
                                 rootAssetId?: string,
                                 nodeId?: string)
    {
        let deviceElemId = significantElement?.model.sysId;
        switch (this.config.clickBehavior?.type)
        {
            case Models.InteractionBehaviorType.None:
                break;

            case Models.InteractionBehaviorType.NavigateDataExplorer:
                const ui = this.app.ui;
                if (this.config.groups.length)
                {
                    if (deviceElemId)
                    {
                        await DataExplorerPageComponent.visualizeDeviceElement(this, ui.navigation, ui.viewstate, deviceElemId, undefined, range);
                    }
                }
                else if (this.config.graph && rootAssetId && nodeId)
                {
                    await this.visualizeAssetGraphChart(range, rootAssetId, nodeId);
                }
                break;

            case Models.InteractionBehaviorType.Pane:
                if (this.paneGraphResponse)
                {
                    let relevantResponse = this.paneGraphResponse.findValidResponse(deviceElemId) || this.paneGraphResponse.findValidResponse(rootAssetId);
                    if (relevantResponse)
                    {
                        let pane = await this.app.domain.panes.getPane(this.paneConfig, relevantResponse);
                        if (pane)
                        {
                            this.paneRange  = this.app.domain.panes.getPaneRange(range);
                            this.paneModels = [pane];
                            this.detectChanges();
                            this.paneOverlay.toggleOverlay();
                        }
                    }

                    break;
                }

            // fall through
            case Models.InteractionBehaviorType.Standard:
                if (significantElement) DeviceElementsDetailPageComponent.navigate(this.app, significantElement);
                break;
        }
    }

    private async visualizeAssetGraphChart(range: Models.RangeSelection,
                                           rootAssetId: string,
                                           nodeId: string)
    {
        const ui            = this.app.ui;
        const config        = TimeSeriesChartConfigurationExtended.newModel();
        config.type         = Models.TimeSeriesChartType.GRAPH;
        const graphId       = UUID.UUID();
        const sharedGraph   = SharedAssetGraphExtended.newModel(this.config.graph, graphId, "Data Aggregation Structure");
        const context       = Models.AssetGraphContextAsset.newInstance({
                                                                            graphId: graphId,
                                                                            nodeId : this.config.graph.nodes[0].id,
                                                                            sysId  : rootAssetId
                                                                        });
        config.graph        = Models.TimeSeriesGraphConfiguration.newInstance({
                                                                                  sharedGraphs: [sharedGraph],
                                                                                  contexts    : [context]
                                                                              });
        const sourceBinding = Models.AssetGraphBinding.newInstance({
                                                                       graphId: graphId,
                                                                       nodeId : nodeId
                                                                   });
        const configExt     = await TimeSeriesChartConfigurationExtended.newInstance(this.app, config);
        await configExt.applyStandardGraphSourceChanges([sourceBinding]);

        await DataExplorerPageComponent.visualizeCharts(ui.navigation, ui.viewstate, [configExt], range);
    }

    public showMode(mode: Models.HierarchicalVisualizationType): void
    {
        if (mode === this.overrideMode) return;

        this.overrideMode = mode;
        this.updateViewport();
    }

    public showRange(range: AggregationGroupsTimeRange): void
    {
        let aggregationGroups = this.aggregationGroups;
        if (aggregationGroups)
        {
            this.selectedRange = aggregationGroups.processedTimeRanges.indexOf(range);
            this.markForCheck();
        }
    }

    public showMenuLegend(): boolean
    {
        return this.isGroupsVisualization() && !this.config.visualizationLegend;
    }

    public showMenuRanges(): boolean
    {
        return this.isGroupsVisualization() && !this.config.visualizationRanges;
    }

    public isRangeSelected(range: AggregationGroupsTimeRange): boolean
    {
        let aggregationGroups = this.aggregationGroups;
        return aggregationGroups && aggregationGroups.processedTimeRanges.indexOf(range) === aggregationGroups.rangeIdx;
    }

    public isModeSelected(mode: Models.HierarchicalVisualizationType): boolean
    {
        let actual = this.overrideMode || this.config.visualizationMode;
        return actual === mode;
    }

    private isGroupsVisualization(): boolean
    {
        let mode = this.overrideMode || this.config.visualizationMode;
        return mode !== Models.HierarchicalVisualizationType.TABLE && mode !== Models.HierarchicalVisualizationType.TABLE_WITH_BAR;
    }

    public exportToExcel(): void
    {
        let fileName          = DownloadDialogComponent.fileName("aggregation_widget__" + this.config.name, ".xlsx");
        const sheetName       = "Aggregation Results";
        let downloadGenerator = new AggregationGroupDownloadGenerator(this.app.domain.apis.exports, fileName, sheetName, this.aggregationGroups);
        DownloadDialogComponent.openWithGenerator(this, "Aggregation Widget CSV", fileName, downloadGenerator);
    }

    private updateViewport(): void
    {
        this.viewport = new Vector2(this.widthRaw, this.heightRaw);
        this.markForCheck();
    }

    protected dimensionsUpdated(): void
    {
        this.viewportUpdateDebouncer.next();

        super.dimensionsUpdated();
    }

    public async refreshSize(): Promise<boolean>
    {
        let loaded               = await this.dataAggregation.refreshSize();
        this.needsContentRefresh = this.dataAggregation.needsContentRefresh;
        return loaded;
    }

    public async refreshContent(): Promise<void>
    {
        await super.refreshContent();
        await this.dataAggregation.refreshContent();
    }

    protected getClipboardData(): ClipboardEntryData<Models.AggregationTableWidgetConfiguration, Models.ReportLayoutItem>
    {
        let model = Models.AggregationTableWidgetConfiguration.deepClone(this.config);

        return new class extends ClipboardEntryData<Models.AggregationTableWidgetConfiguration, Models.ReportLayoutItem>
        {
            private m_sharedGraphs: Models.SharedAssetGraph[] = [];
            private readonly m_type: DataAggregationType;

            constructor()
            {
                super("aggregation table");

                if (model.columns?.length && model.graph)
                {
                    this.m_sharedGraphs.push(SharedAssetGraphExtended.newModel(model.graph, null, null));
                    this.m_type = DataAggregationType.Bindings;
                }
                else
                {
                    this.m_sharedGraphs = ControlPointsGroupExtended.getLocalGraphs(model.groups);
                    this.m_type         = DataAggregationType.Groups;
                }
            }

            public getDashboardWidget(): Models.AggregationTableWidgetConfiguration
            {
                return Models.AggregationTableWidgetConfiguration.deepClone(model);
            }

            public getReportItem(oldToNewGraphId: Lookup<string>): Models.ReportLayoutItem
            {
                let graphId: string;
                let groups: Models.ControlPointsGroup[] = [];
                switch (this.m_type)
                {
                    case DataAggregationType.Bindings:
                        if (this.m_sharedGraphs?.length !== 1) return null;
                        if (oldToNewGraphId) graphId = oldToNewGraphId[this.m_sharedGraphs[0].id];
                        break;

                    case DataAggregationType.Groups:
                        if (!model.groups) return null;
                        groups = ClipboardEntryData.getReportGroups(model.groups, this.m_sharedGraphs, oldToNewGraphId);
                        if (groups.length !== model.groups.length) return null;
                        break;
                }

                let element = Models.CustomReportElementAggregationTable.newInstance({
                                                                                         label                  : model.name,
                                                                                         groups                 : groups,
                                                                                         columns                : model.columns?.map((column) => Models.AggregationNodeBinding.deepClone(column))
                                                                                                                  || [],
                                                                                         graphId                : graphId,
                                                                                         initialSort            : Models.SortCriteria.newInstance({ascending: true}),
                                                                                         controlPointDisplayType: model.controlPointDisplayType,
                                                                                         visualizationMode      : model.visualizationMode
                                                                                     });
                return Models.ReportLayoutItem.newInstance({element: element});
            }

            public getReportGraphs(): Models.SharedAssetGraph[]
            {
                return UtilsService.arrayCopy(this.m_sharedGraphs);
            }

        }();
    }
}

@WidgetDef({
               friendlyName      : "Data Aggregation",
               typeName          : "AGGREGATION_TABLE",
               model             : Models.AggregationTableWidgetConfiguration,
               component         : AggregationTableWidgetComponent,
               classes           : ["scrollable"],
               dashboardCreatable: true,
               subgroupCreatable : true,
               maximizable       : true,
               defaultWidth      : 6,
               defaultHeight     : 4,
               hostScalableText  : false,
               needsProtector    : true,
               documentation     : {
                   description: "The Data Aggregation widget allows you to aggregate your selected control points and then view them in a visualization or table",
                   examples   : [
                       {
                           file       : "widgets/AGGREGATION_TABLE/table.png",
                           label      : "Table With Bars",
                           description: "A group of control points in a tabular format with bars and 3 time ranges, shown as a 6x6 widget."
                       },
                       {
                           file       : "widgets/AGGREGATION_TABLE/sunburst.png",
                           label      : "Sunburst Chart",
                           description: "A hierarchical set of control points as a sunburst chart (hierarchical version of a donut chart), shown as a 6x5 widget."
                       },
                       {
                           file       : "widgets/AGGREGATION_TABLE/binding-table.png",
                           label      : "Asset Structure Binding Table",
                           description: "Aggregated values from control points matched by an asset structure search represented in a table."
                       },
                       {
                           file       : "widgets/AGGREGATION_TABLE/bubble.png",
                           label      : "Bubble Chart",
                           description: "Two groups of control points as a bubble chart, shown as a 6x6 widget."
                       },
                       {
                           file       : "widgets/AGGREGATION_TABLE/pie.png",
                           label      : "Pie Chart",
                           description: "Two groups of control points as a pie chart, shown as a 6x6 widget."
                       }
                   ]
               }
           })
export class AggregationTableWidgetConfigurationExtended extends WidgetConfigurationExtended<Models.AggregationTableWidgetConfiguration>
{
    protected initializeForWizardInner()
    {
        let model                     = this.model;
        model.visualizationMode       = Models.HierarchicalVisualizationType.TABLE;
        model.controlPointDisplayType = Models.ControlPointDisplayType.NameOnly;
        model.isolateGroupRanges      = true;
        model.clickBehavior           = Models.InteractionBehavior.newInstance({type: Models.InteractionBehaviorType.Standard});
        model.filterableRanges        = [
            Models.FilterableTimeRange.newInstance({range: RangeSelectionExtended.newModel(Models.TimeRangeId.Last24Hours)}),
            Models.FilterableTimeRange.newInstance({range: RangeSelectionExtended.newModel(Models.TimeRangeId.Last7Days)}),
            Models.FilterableTimeRange.newInstance({range: RangeSelectionExtended.newModel(Models.TimeRangeId.Last30Days)})
        ];

        const group   = ControlPointsGroupExtended.newModel({selections: Models.ControlPointsSelection.newInstance({identities: []})});
        model.groups  = [group];
        model.columns = [];
    }

    public startingStep(): string
    {
        return this.model.groups.length ? "cp-grouping" : "graphs";
    }

    public getBindings(): Models.AssetGraphBinding[]
    {
        return [];
    }
}
