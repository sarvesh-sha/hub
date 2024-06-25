import {CdkOverlayOrigin} from "@angular/cdk/overlay";
import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Injector, Input, Output, Renderer2, ViewChild} from "@angular/core";

import {DeletionAffectedAnnotation, InteractableSource, InteractableSourcesChart, SourceModificationResult, TimeSeriesChartConfigurationExtended, TimeSeriesChartHandler, TimeSeriesSourceConfigurationExtended, TimeSeriesSourceHost} from "app/customer/visualization/time-series-utils";
import {AssetGraphTreeNode, SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {AssetExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {UnitsService} from "app/services/domain/units.service";
import * as Models from "app/services/proxy/model/models";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";
import {DataSourceWizardDialogComponent, DataSourceWizardPurpose, DataSourceWizardState} from "app/shared/charting/data-source-wizard/data-source-wizard-dialog.component";
import {HierarchicalVisualizationComponent} from "app/shared/charting/hierarchical-visualization/hierarchical-visualization.component";
import {PivotTable} from "app/shared/charting/interactive-tree/interactive-tree.component";
import {ScatterPlotContainerComponent} from "app/shared/charting/scatter-plot/scatter-plot-container.component";
import {ConsolidatedSourceChipComponent} from "app/shared/charting/source-chip/consolidated-source-chip.component";
import {SourceAction} from "app/shared/charting/source-chip/source-chip.component";
import {TimeSeriesChartComponent} from "app/shared/charting/time-series-chart/time-series-chart.component";
import {TimeSeriesDownloader} from "app/shared/charting/time-series-downloader";
import {ColorConfigurationExtended} from "app/shared/colors/color-configuration-extended";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";
import {GpsMapComponent} from "app/shared/mapping/gps-map/gps-map.component";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {CanvasZoneSelection} from "framework/ui/charting/app-charting-utilities";
import {ChartRangeSelectionHandler} from "framework/ui/charting/chart.component";
import {VisualizationDataSourceState} from "framework/ui/charting/core/data-sources";
import {ChartTimeRange} from "framework/ui/charting/core/time";
import {VerticalViewWindow} from "framework/ui/charting/vertical-view-window";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {SelectComponent} from "framework/ui/forms/select.component";
import {OverlayComponent} from "framework/ui/overlays/overlay.component";
import {mapInParallel} from "framework/utils/concurrency";

import {Subscription} from "rxjs";

@Component({
               selector       : "o3-time-series-container",
               templateUrl    : "./time-series-container.component.html",
               styleUrls      : ["./time-series-container.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class TimeSeriesContainerComponent extends SharedSvc.BaseApplicationComponent
{
    table: PivotTable;
    private m_hierarchicalGraphExt: SharedAssetGraphExtended;

    tupleIdsByPanel: string[][];

    private gpsEquipment: AssetExtended[];
    private gpsEquipmentClassId: string;

    minHeight: number = 0;

    @ViewChild(TimeSeriesChartComponent, {static: false}) chartElement: TimeSeriesChartComponent;
    @ViewChild(HierarchicalVisualizationComponent, {static: false}) hierarchyElement: HierarchicalVisualizationComponent;
    @ViewChild(GpsMapComponent, {static: false}) mapElement: GpsMapComponent;
    @ViewChild(ScatterPlotContainerComponent, {static: false}) scatterElement: ScatterPlotContainerComponent;
    @ViewChild(ConsolidatedSourceChipComponent, {static: false}) chips: ConsolidatedSourceChipComponent;

    @ViewChild("sourceConfigurer", {static: true}) sourceConfigurer: OverlayComponent;
    @ViewChild("sourcesDropdownOrigin") sourcesDropdownOrigin: CdkOverlayOrigin;

    @ViewChild("chartHeader") chartHeader: ElementRef;
    @ViewChild("resizeContainer") resizeContainer: ElementRef;

    @ViewChild("test_contexts") test_contexts: SelectComponent<string>;
    @ViewChild("test_addSources", {read: ElementRef}) test_addSources: ElementRef;
    @ViewChild("test_editSources", {read: ElementRef}) test_editSources: ElementRef;
    @ViewChild("test_annotations", {read: ElementRef}) test_annotations: ElementRef;
    @ViewChild("test_colors", {read: ElementRef}) test_colors: ElementRef;
    @ViewChild("test_settings", {read: ElementRef}) test_settings: ElementRef;
    @ViewChild("test_consolidatedChips") test_consolidated: ConsolidatedSourceChipComponent;

    private m_externalSelectionChangedSubs: Subscription[];
    private m_selectionChangedSub: Subscription;
    private m_configExt: TimeSeriesChartConfigurationExtended = TimeSeriesChartConfigurationExtended.emptyInstance(this.app);
    @Input() set configExt(configExt: TimeSeriesChartConfigurationExtended)
    {
        if (configExt !== this.m_configExt)
        {
            if (this.m_selectionChangedSub)
            {
                this.m_selectionChangedSub.unsubscribe();
                this.m_selectionChangedSub = null;
            }

            if (this.m_externalSelectionChangedSubs)
            {
                for (let sub of this.m_externalSelectionChangedSubs) sub.unsubscribe();
                this.m_externalSelectionChangedSubs = null;
            }

            if (configExt)
            {
                this.m_configExt           = configExt;
                this.m_selectionChangedSub = this.subscribeToObservable(configExt.assetSelectionHelper.selectionChanged, () => this.selectedGraphChanged());

                this.m_externalSelectionChangedSubs = this.m_configExt.externalContextUpdaters?.map(
                    (updater) => this.subscribeToObservable(updater.selectionChanged, () => this.markForCheck()));
            }
        }
    }

    get configExt(): TimeSeriesChartConfigurationExtended
    {
        return this.m_configExt;
    }

    @Input() range: Models.RangeSelection;

    m_readonly = false;
    @Input() set readonly(readonly: boolean)
    {
        this.m_readonly = readonly;
        this.updateSourceActions();
    }

    get readonly(): boolean
    {
        return this.m_readonly;
    }

    @Input() externalGraphsHost: GraphConfigurationHost;
    @Input() disableAnnotations  = false;
    @Input() allowDashboardAdd   = true;
    @Input() deletable           = true;
    @Input() canDeleteAllSources = true;
    @Input() resizable           = true;

    @Input() zoomable = true;
    @Input() rangeHandler: ChartRangeSelectionHandler;

    private m_viewWindow: VerticalViewWindow;
    @Input() set viewWindow(viewWindow: VerticalViewWindow)
    {
        if (!viewWindow) return;

        let headerHeight  = this.chartHeader?.nativeElement.offsetHeight || 0;
        this.m_viewWindow = new VerticalViewWindow(viewWindow.viewTop - headerHeight, viewWindow.viewHeight);

        if (this.chips) this.chips.updateConsolidated();
    }

    get viewWindow(): VerticalViewWindow
    {
        return this.m_viewWindow;
    }

    @Input() height: number;
    @Input() handleInteractions       = true;
    @Input() showSourceBar            = true;
    @Input() configurationOnly        = false;
    @Input() printable                = false;
    @Input() embedded                 = false;
    @Input() noSourcesMessage: string = "Add data sources using add button above.";

    @Output() onDelete                   = new EventEmitter<TimeSeriesChartConfigurationExtended>();
    @Output() configExtChange            = new EventEmitter<TimeSeriesChartConfigurationExtended>();
    @Output() chartUpdated               = new EventEmitter<boolean>();
    @Output() sourceStatesUpdated        = new EventEmitter<Lookup<VisualizationDataSourceState>>();
    @Output() sourcesConsolidatedUpdated = new EventEmitter<boolean>();
    @Output() startedFetchingData        = new EventEmitter<void>();
    @Output() stoppedFetchingData        = new EventEmitter<void>();
    @Output() expandRange                = new EventEmitter<ChartTimeRange>();

    private refresherId: number = 0;

    sourceActions: SourceAction[] = [];

    private m_sourcesConsolidated: boolean;
    get sourcesConsolidated(): boolean
    {
        return this.m_sourcesConsolidated;
    }

    get interactableChart(): InteractableSourcesChart
    {
        return this.configExt.chartHandler.getInteractableChart(this);
    }

    hideSourcePillsOverride: boolean;

    get showSources(): boolean
    {
        if (!this.interactableChart?.isReady()) return false;
        if (!this.showSourceBar) return false;
        if (this.hideSourcePillsOverride != null) return !this.hideSourcePillsOverride;

        return this.configExt.chartHandler.showSourcePills();
    }

    get consolidatedChipTooltip(): string
    {
        return this.configExt.chartHandler.consolidatedChipTooltip();
    }

    get dataSourceType(): string
    {
        return this.configExt.chartHandler.dataSourceType();
    }

    get sourcesAreDeletable(): boolean
    {
        if (this.readonly) return false;
        if (!this.isLine) return false;

        if (this.canDeleteAllSources) return true;
        return 1 < this.configExt.sourcesExt.reduce((cum,
                                                     curr) => curr.deleted || curr.markedForDeletion ? cum : cum + 1, 0);
    }

    get hasNoPadding(): boolean
    {
        return !this.configExt.chartHandler.withPadding(this.embedded);
    }

    get hideAnnotationsButton(): boolean
    {
        if (this.disableAnnotations) return true;
        if (this.configurationOnly) return true;
        if (!this.hasSources) return true;

        return !this.isLine;
    }

    get hasSources(): boolean
    {
        return this.configExt.chartHandler.hasSources();
    }

    get isStandardLine(): boolean
    {
        return !this.configExt.model.type || this.configExt.model.type === Models.TimeSeriesChartType.STANDARD;
    }

    get isAssetStructureLine(): boolean
    {
        return this.configExt.model.type === Models.TimeSeriesChartType.GRAPH;
    }

    get isLine(): boolean
    {
        return this.isStandardLine || this.isAssetStructureLine;
    }

    get isHierarchical(): boolean
    {
        return this.configExt.model.type === Models.TimeSeriesChartType.HIERARCHICAL;
    }

    get isMap(): boolean
    {
        return this.configExt.model.type === Models.TimeSeriesChartType.COORDINATE;
    }

    get isStandardScatter(): boolean
    {
        return this.configExt.model.type === Models.TimeSeriesChartType.SCATTER;
    }

    get isAssetStructureScatter(): boolean
    {
        return this.configExt.model.type === Models.TimeSeriesChartType.GRAPH_SCATTER;
    }

    get usesAssetStructure(): boolean
    {
        if (this.isAssetStructureScatter) return true;
        if (this.isMap) return true;
        if (this.isAssetStructureLine) return true;
        if (this.isStandardLine && this.configExt.model.graph) return true;

        return false;
    }

    get isScatter(): boolean
    {
        return this.isStandardScatter || this.isAssetStructureScatter;
    }

    get editableColors(): boolean
    {
        return this.configExt.chartHandler.editableColors();
    }

    get exportablePng(): boolean
    {
        return this.configExt.chartHandler.exportablePng();
    }

    get isResizable(): boolean
    {
        if (!this.resizable) return false;
        if (this.isHierarchical)
        {
            return this.configExt.model.hierarchy?.bindings?.some((binding) => binding?.options?.sizing === Models.HierarchicalVisualizationSizing.FIT);
        }
        return true;
    }

    private readonly host: TimeSeriesSourceHost;

    get componentHeight(): number
    {
        return this.m_element.nativeElement.clientHeight;
    }

    private m_updating: boolean;
    get updating(): boolean
    {
        return this.m_updating;
    }

    get element(): ElementRef
    {
        return this.m_element;
    }

    constructor(inj: Injector,
                private m_element: ElementRef,
                private m_renderer: Renderer2)
    {
        super(inj);

        this.host = new TimeSeriesSourceHost(this);
    }

    //--//

    async ngOnInit()
    {
        super.ngOnInit();

        // Sync to chart component
        this.updateSources();
    }

    ngOnDestroy(): void
    {
        super.ngOnDestroy();

        this.refresherId = -1;
    }

    protected afterLayoutChange(): void
    {
        super.afterLayoutChange();

        if (this.configExt) this.refreshSize();
    }

    async lineChartChanged()
    {
        this.hideSourcePillsOverride = undefined;
        this.emitChange();
        if (this.isLine && this.usesAssetStructure)
        {
            await this.updateSources();
        }
        this.refreshSize();
    }

    private updateSourceActions()
    {
        if (!this.m_readonly && !this.isAssetStructureScatter && !this.isMap)
        {
            const isEnabledFn  = this.isScatter ? (source: InteractableSource) => !source.colorStops : undefined;
            this.sourceActions = [new SourceAction("settings", (source) => this.configureSource(source), undefined, isEnabledFn)];
        }
        else
        {
            this.sourceActions = [];
        }
    }

    updateConsolidated(consolidated: boolean)
    {
        this.m_sourcesConsolidated = consolidated;
        this.sourcesConsolidatedUpdated.emit(consolidated);
    }

    onMouseMove(x: number,
                y: number): boolean
    {
        let chart = this.configExt.chartHandler.getChartingElement(this);
        return chart?.onMouseMove(x, y);
    }

    onMouseLeave()
    {
        let chart = this.configExt.chartHandler.getChartingElement(this);
        chart?.onMouseLeave();
    }

    configureSource(source: InteractableSource)
    {
        this.interactableChart.configureSource(source.identifier);
    }

    toggleConfig(): void
    {
        let chart = this.configExt.chartHandler.getChartingElement(this);
        chart?.toggleConfigurer();
    }

    toggleColorConfig(): void
    {
        let chart = this.configExt.chartHandler.getChartingElement(this);
        chart?.toggleColorConfigurer();
    }

    async exportVisualizationPNG()
    {
        let chartElement = this.hasSources && this.configExt.chartHandler.getChartingElement(this);

        let canvasPNG = chartElement?.getCanvasPNG();
        if (!canvasPNG) return;

        let canvasTitle = chartElement.getCanvasTitle();

        let rangeSelection = new RangeSelectionExtended(this.range);
        let minDate        = MomentHelper.fileNameFormat(rangeSelection.getMin());
        let maxDate        = MomentHelper.fileNameFormat(rangeSelection.getMax());
        let fileName       = `${canvasTitle}_${minDate}-${maxDate}.png`;
        DownloadDialogComponent.openWithUrl(this, "Download Chart Image", fileName, canvasPNG);
    }

    emitChange()
    {
        this.configExtChange.emit(this.configExt);
        this.chartUpdated.emit(true);
    }

    isEmpty(): boolean
    {
        return !this.configExt.chartHandler.hasSources();
    }

    async configureSources(asOverride?: boolean): Promise<void>
    {
        let override: Models.TimeSeriesChartType;
        if (asOverride)
        {
            override = this.configExt.model.type === Models.TimeSeriesChartType.STANDARD ? Models.TimeSeriesChartType.GRAPH : Models.TimeSeriesChartType.STANDARD;
        }

        let isNew                          = !override && !TimeSeriesChartHandler.newInstance(this.configExt)
                                                                                 .hasSources();
        let dataSourceWizardState          = new DataSourceWizardState(isNew, DataSourceWizardPurpose.visualization, this.configExt, this.externalGraphsHost, false);
        dataSourceWizardState.overrideType = override;
        await dataSourceWizardState.updateForChart(this.configExt);

        if (await DataSourceWizardDialogComponent.open(dataSourceWizardState, this))
        {
            await this.updateConfig(dataSourceWizardState);
        }
    }

    async updateConfig(dataSourceWizardState: DataSourceWizardState)
    {
        this.m_updating = true;

        let chart = this.configExt.model;

        chart.type = dataSourceWizardState.type;
        this.updateSourceActions();
        switch (chart.type)
        {
            case Models.TimeSeriesChartType.HIERARCHICAL:
                chart.hierarchy = dataSourceWizardState.hierarchy;
                for (let binding of chart.hierarchy.bindings)
                {
                    if (!binding.color) binding.color = ColorConfigurationExtended.newModel();
                    if (!binding.options) binding.options = TimeSeriesChartConfigurationExtended.defaultHierarchicalOptions();
                }
                break;

            case Models.TimeSeriesChartType.COORDINATE:
                chart.palette = "Map Path Colors";
                break;

            case Models.TimeSeriesChartType.STANDARD:
                await this.changeSources(dataSourceWizardState.ids);
                if (dataSourceWizardState.overrideType === Models.TimeSeriesChartType.GRAPH)
                {
                    await this.changeGraphSources(dataSourceWizardState.graphBindings);
                }
                break;

            case Models.TimeSeriesChartType.SCATTER:
                chart.graph = null;
            // fall through

            case Models.TimeSeriesChartType.GRAPH_SCATTER:
                chart.scatterPlot.sourceTuples = dataSourceWizardState.sourceTuples;
                chart.panels                   = dataSourceWizardState.panels;

                let currPanelIdx = -1;
                let hasZ;
                // clear invalid color setups
                for (let tuple of chart.scatterPlot.sourceTuples)
                {
                    if (tuple.panel > currPanelIdx)
                    {
                        currPanelIdx++;
                        hasZ = chart.type === Models.TimeSeriesChartType.SCATTER ? !!tuple.sourceZ.deviceElementId : !!tuple.sourceZ.binding?.nodeId;
                    }

                    if (tuple.colorOverride && hasZ) tuple.colorOverride = undefined;
                }

                if (chart.type === Models.TimeSeriesChartType.SCATTER) break;

                await this.changeGraphSources(dataSourceWizardState.graphBindings);
                break;

            case Models.TimeSeriesChartType.GRAPH:
                if (dataSourceWizardState.overrideType === Models.TimeSeriesChartType.STANDARD)
                {
                    await this.changeSources(dataSourceWizardState.ids, true);
                }

                let idToBinding                                           = new Map<string, Models.AssetGraphBinding>();
                let graphIdToBindings: Lookup<Models.AssetGraphBinding[]> = {};
                for (let binding of dataSourceWizardState.graphBindings)
                {
                    let bindings = graphIdToBindings[binding.graphId];
                    if (!bindings)
                    {
                        bindings = graphIdToBindings[binding.graphId] = [];
                    }
                    bindings.push(binding);

                    idToBinding.set(AssetGraphTreeNode.getIdFromBinding(binding), binding);
                }
                for (let binding of dataSourceWizardState.initialExternalBindings)
                {
                    let id = AssetGraphTreeNode.getIdFromBinding(binding);
                    if (idToBinding.has(id))
                    {
                        // use the old external binding so as to maintain any previously selected selectorId
                        idToBinding.set(id, binding);
                        for (let likeBinding of graphIdToBindings[binding.graphId] || [])
                        {
                            likeBinding.selectorId = binding.selectorId;
                        }
                    }
                }

                await this.changeGraphSources([...idToBinding.values()]);
                break;
        }

        await this.updateSources();
        this.chartUpdated.emit(true);

        this.m_updating = false;
    }

    async addWidget(title: string)
    {
        let config = Models.TimeSeriesWidgetConfiguration.newInstance({
                                                                          name  : title,
                                                                          range : this.range,
                                                                          charts: [this.configExt.model]
                                                                      });

        let cfg = await this.app.domain.dashboard.getActive();
        await cfg.addTimeSeriesWidget(config);
    }

    async delete(): Promise<void>
    {
        if (!this.hasSources || await this.confirmOperation("This visualization cannot be recovered."))
        {
            this.onDelete.emit(this.configExt);
        }
    }

    chartHeight(defaultHeight: number = 200): number
    {
        let height = this.height || this.configExt.model.display.size || defaultHeight;
        return this.isHierarchical ? Math.max(this.minHeight, height) : height;
    }

    smallest(): number
    {
        return this.isHierarchical ? this.minHeight : 200;
    }

    updateChartHeight(height: number)
    {
        if (height === this.configExt.model.display.size) return;

        this.configExt.model.display.size = height;
        this.detectChanges();
        this.chartUpdated.emit(true);
    }

    refreshSize(retries: number = 0): boolean
    {
        const refreshFn = () => !!this.configExt.chartHandler.getChartingElement(this)
                                      ?.refreshSize();
        let refreshed   = refreshFn();
        if (!refreshed)
        {
            let refresherId = ++this.refresherId;
            UtilsService.executeWithRetries(async () => refresherId !== this.refresherId || refreshFn(), retries, 1000, undefined, 1, true);
        }

        return refreshed;
    }

    refreshZoomability()
    {
        this.chartElement?.updateZoomability(true);
        this.mapElement?.updateZoomability();
    }

    resetResizeContainer()
    {
        if (!this.isResizable && this.resizeContainer) this.m_renderer.removeStyle(this.resizeContainer.nativeElement, "height");

        this.chartUpdated.emit();
    }

    async exportToExcel()
    {
        let elements = await this.configExt.chartHandler.getDownloaderElements(this.app);
        if (!elements) return;

        let rangeSelection = new RangeSelectionExtended(this.range);
        let dataDownloader = new TimeSeriesDownloader(this.app.domain, elements, rangeSelection.getMin(), rangeSelection.getMax(), this.range.zone);
        let minDate        = MomentHelper.fileNameFormat(rangeSelection.getMin());
        let maxDate        = MomentHelper.fileNameFormat(rangeSelection.getMax());
        let fileName       = `VisualizationData__${minDate}-${maxDate}.xlsx`;
        DownloadDialogComponent.openWithGenerator(this, "Export Data", fileName, dataDownloader);
    }

    async selectedGraphChanged()
    {
        if (this.usesAssetStructure && !this.isMap)
        {
            this.updateStandardGraphContext();
        }

        await this.updateSources();
    }

    async sourcesOverlayOpened()
    {
        if (this.sourcesAreDeletable)
        {
            mapInParallel(this.configExt.sourcesExt, async (source) =>
            {
                if (await source.cancelDeletion(this.app.domain.units, this.chartElement))
                {
                    await this.deleteSource(source);
                }
            });
        }
    }

    async deleteSource(source: TimeSeriesSourceConfigurationExtended)
    {
        if (!this.isLine || source.model.pointBinding) return;

        let panelExt     = source.ownerPanel;
        let unitsFactors = source.unitsFactors;

        source.affectedAnnotations                        = [];
        let panelAnnotations                              = this.chartElement.annotations.filter((annotation) => annotation.panel === panelExt.index);
        let groupLinkedAnnotations: CanvasZoneSelection[] = [];
        if (UnitsService.areEquivalent(panelExt.leftAxisExtended.model.displayFactors, unitsFactors))
        {
            // group linked annotations must be associated with axis' group (group with same unitsFactors as the axis' displayFactors)
            groupLinkedAnnotations = panelAnnotations.filter((annotation) => !annotation.chartSource);
            if (groupLinkedAnnotations.length > 0)
            {
                for (let annotation of groupLinkedAnnotations)
                {
                    let annotationIdx = this.chartElement.annotations.indexOf(annotation);
                    source.affectedAnnotations.push(new DeletionAffectedAnnotation(annotationIdx, annotation, panelExt.owner.model.annotations[annotationIdx], unitsFactors));
                }

                let sameGroupSources = await panelExt.filterSources(this.app.domain.units, panelExt.leftAxisExtended, unitsFactors, source);
                if (sameGroupSources?.some((source) => !source.markedForDeletion)) groupLinkedAnnotations = [];
            }
        }

        let chartSource             = source.getChartData();
        let sourceLinkedAnnotations = panelAnnotations.filter((annotation) => annotation.chartSource == chartSource);
        for (let annotation of sourceLinkedAnnotations)
        {
            let annotationIdx = this.chartElement.annotations.indexOf(annotation);
            source.affectedAnnotations.push(new DeletionAffectedAnnotation(annotationIdx, annotation, panelExt.owner.model.annotations[annotationIdx]));
        }

        source.affectedAnnotations.sort((a,
                                         b) => UtilsService.compareNumbers(a.idx, b.idx, true));
        if (groupLinkedAnnotations.length > 0 || sourceLinkedAnnotations.length > 0)
        {
            let confirmed = await this.confirmOperation("Deleting this source will invalidate some of your annotations.");
            if (!confirmed) return;
        }

        await source.markAsDeleted(this.app, this.chartElement, groupLinkedAnnotations.length > 0);
    }

    async cancelSourceDelete(source: TimeSeriesSourceConfigurationExtended)
    {
        source.cancelDeletion(this.app.domain.units, this.chartElement);
    }

    private updateStandardGraphContext()
    {
        let contexts: Models.AssetGraphContext[] = [];

        this.configExt.assetSelectionHelper.forEachAssetNodeSelection((graphId,
                                                                       rootNodeId,
                                                                       entries) =>
                                                                      {
                                                                          contexts.push(Models.AssetGraphContextAssets.newInstance({
                                                                                                                                       graphId: graphId,
                                                                                                                                       nodeId : rootNodeId,
                                                                                                                                       sysIds : entries
                                                                                                                                   }));
                                                                      });

        this.configExt.model.graph.contexts = contexts;
    }

    private async updateSources()
    {
        this.table = null;

        let chart = this.configExt.model;

        if (this.isHierarchical)
        {
            if (!UtilsService.compareJson(this.m_hierarchicalGraphExt?.model, this.configExt.hierarchicalGraph))
            {
                this.m_hierarchicalGraphExt = new SharedAssetGraphExtended(this.app.domain, Models.SharedAssetGraph.deepClone({graph: this.configExt.hierarchicalGraph}));
            }

            const response      = await this.m_hierarchicalGraphExt.resolve();
            const leafNodeIds   = this.configExt.model.hierarchy.bindings.map((binding) => binding.leafNodeId);
            const graphBindings = leafNodeIds.map((leafNodeId) => Models.AssetGraphBinding.newInstance({nodeId: leafNodeId}));
            const tuples        = response.resolveBindingTuples(graphBindings, true);
            const table         = new PivotTable(tuples, chart.hierarchy.virtualNodes, this.m_hierarchicalGraphExt, leafNodeIds);
            await table.load(this.app);
            this.table = table;
        }
        else if (!chart.hierarchy)
        {
            chart.hierarchy = new Models.HierarchicalVisualization();
        }

        if (this.usesAssetStructure && !this.isMap)
        {
            await this.loadGraphs();

            this.configExt.assetSelectionHelper.setAllAssetNodeSelections(this.collectGraphContext(chart.graph.contexts));

            await this.changeGraphSources(this.isLine && this.configExt.standardGraphBindingSet());
        }
        else if (this.isMap)
        {
            await this.ensureGpsInfo();
            await this.configExt.assetSelectionHelper.ensureGpsSelection(async () =>
                                                                         {
                                                                             if (this.configExt.dataSources.length > 0)
                                                                             {
                                                                                 let mapSources = this.configExt.mapSources;
                                                                                 if (!mapSources)
                                                                                 {
                                                                                     await this.configExt.setMapSources();
                                                                                     mapSources = this.configExt.mapSources || [];
                                                                                 }

                                                                                 return mapSources.map((gpsAsset) => gpsAsset.model.sysId);
                                                                             }

                                                                             return [];
                                                                         });
            this.markForCheck();

            let gpsIds: string[] = [];
            this.configExt.assetSelectionHelper.forEachGpsSelection((gpsId) => gpsIds.push(gpsId));
            let gpsDataSources = await this.configExt.getCoordinateDataSourceIds(gpsIds);
            await this.configExt.changeGpsSources(gpsDataSources);
        }
        else if (this.isStandardScatter)
        {
            this.configExt.configChanged.next(false);
        }

        // Trigger change
        this.chartElement?.onChange();
        this.scatterElement?.onChange();

        this.detectChanges();
    }

    handleChartUpdated(sourcesChanged: boolean)
    {
        this.chartUpdated.emit(sourcesChanged);
    }

    updateTupleIds(tuplesChanged: boolean)
    {
        this.tupleIdsByPanel = this.scatterElement.tupleIdsByPanel();

        this.handleChartUpdated(tuplesChanged);
    }

    private collectGraphContext(contexts: Models.AssetGraphContext[]): Map<Models.AssetGraphBinding, string[]>
    {
        let selections = new Map<Models.AssetGraphBinding, string[]>();
        for (let graphContext of contexts || [])
        {
            const graphId = graphContext.graphId;
            const rootId  = graphContext.nodeId;
            if (!rootId || !graphId) continue;

            const binding = Models.AssetGraphBinding.newInstance({
                                                                     graphId: graphId,
                                                                     nodeId : rootId
                                                                 });

            if (graphContext instanceof Models.AssetGraphContextAsset)
            {
                selections.set(binding, [graphContext.sysId]);
            }
            else if (graphContext instanceof Models.AssetGraphContextAssets)
            {
                if (graphContext.sysIds)
                {
                    selections.set(binding, graphContext.sysIds);
                }
                else if (graphContext.selectAll)
                {
                    selections.set(binding, this.configExt.assetSelectionHelper.getAssetOptions(binding)
                                                .map((o) => o.id));
                }
            }
        }

        return selections;
    }

    private async changeSources(ids: Models.RecordIdentity[],
                                leaveGraphSources?: boolean): Promise<SourceModificationResult>
    {
        let oldSet = new Set<string>(this.configExt.dataSources.map((source) => source.id));
        let newSet = new Set<string>(ids.map((id) => id.sysId));

        let idsToBeAdded = ids.filter((record) => !oldSet.has(record.sysId))
                              .map((record) => record.sysId);

        let idsToBeRemoved = Array.from(oldSet)
                                  .filter((id) => !newSet.has(id));

        let sourcesToBeAdded = await mapInParallel(idsToBeAdded, (id: string) => TimeSeriesSourceConfigurationExtended.resolveFromIdAndDimension(this.host, id));

        let sourcesToBeRemoved: TimeSeriesSourceConfigurationExtended[] = [];
        for (let id of idsToBeRemoved)
        {
            for (let source of this.configExt.getSourcesById(id) || [])
            {
                if (!leaveGraphSources || !source.model.pointBinding)
                {
                    sourcesToBeRemoved.push(source);
                }
            }
        }

        let removedIdentifiers  = new Set(sourcesToBeRemoved.map((sourceExt) => sourceExt.identifier));
        let leftoverAnnotations = this.configExt.model.annotations.filter((annotation) => !annotation.sourceId || !removedIdentifiers.has(annotation.sourceId));
        if (leftoverAnnotations.length < this.configExt.model.annotations.length)
        {
            if (!await this.confirmOperation("This will invalidate some of your annotations."))
            {
                return new SourceModificationResult();
            }

            this.configExt.model.annotations = leftoverAnnotations;
        }

        // Apply the desired changes
        let result = await this.configExt.applySourceChanges(sourcesToBeAdded, sourcesToBeRemoved);

        // Notify if some sources were ignored
        if (result.ignored.length > 0) this.app.framework.errors.success(`${result.ignored.length} control points were not added due to lack of data`, -1);

        return result;
    }

    private async changeGraphSources(bindings?: Models.AssetGraphBinding[])
    {
        await this.loadGraphs();

        this.updateStandardGraphContext();

        if (this.isLine && this.usesAssetStructure && bindings)
        {
            if (this.externalGraphsHost)
            {
                let externals                               = UtilsService.extractLookup(this.externalGraphsHost.getGraphs());
                this.configExt.model.graph.externalBindings = bindings.filter((binding) => externals[binding.graphId]);
            }

            let result = await this.configExt.applyStandardGraphSourceChanges(bindings);

            // Notify if some sources were ignored
            if (result.ignored.length > 0) this.app.framework.errors.success(`${result.ignored.length} control points were not added due to lack of data`, -1);
            this.updateGraphOptions(bindings);
        }
        else if (this.isAssetStructureScatter)
        {
            let bindings: Models.AssetGraphBinding[] = [];
            for (let tuple of this.configExt.model.scatterPlot.sourceTuples)
            {
                bindings.push(tuple.sourceX?.binding);
                bindings.push(tuple.sourceY?.binding);
                bindings.push(tuple.sourceZ?.binding);
            }
            this.updateGraphOptions(bindings);

            this.configExt.configChanged.next(false);
        }
    }

    private async loadGraphs()
    {
        await this.configExt.loadGraphs();

        if (this.usesAssetStructure && !this.isMap)
        {
            this.configExt.assetSelectionHelper.setAssetOptions(await this.configExt.getGraphControlOptions());
        }
    }

    private updateGraphOptions(bindings: Models.AssetGraphBinding[])
    {
        let referencedRoots: Models.AssetGraphBinding[] = [];

        for (const sharedGraph of this.configExt?.model.graph?.sharedGraphs || [])
        {
            const graphExt = this.configExt.resolvedGraphs.get(sharedGraph.id);

            const roots = new Set<string>();

            for (let binding of bindings)
            {
                if (binding && binding.graphId === sharedGraph.id && graphExt.getNodeById(binding.nodeId))
                {
                    const root = graphExt.getRootNodeId(binding.nodeId);
                    if (!roots.has(root))
                    {
                        roots.add(root);
                        referencedRoots.push(Models.AssetGraphBinding.newInstance({
                                                                                      graphId: sharedGraph.id,
                                                                                      nodeId : root
                                                                                  }));
                    }
                }
            }
        }

        this.configExt.assetSelectionHelper.resolveAssetOptions(referencedRoots, (graph) => this.configExt.resolvedGraphs.get(graph).name);
    }

    private async ensureGpsInfo()
    {
        if (!this.gpsEquipmentClassId)
        {
            this.gpsEquipmentClassId = await this.app.domain.normalization.getWellKnownEquipmentClassId(Models.WellKnownEquipmentClass.GPS);
        }

        if (!this.gpsEquipment)
        {
            let gpsRecords    = await TimeSeriesChartConfigurationExtended.getAvailableGps(this.app.domain);
            this.gpsEquipment = await this.app.domain.assets.getTypedPage(AssetExtended, gpsRecords, 0, 1000);

        }

        await this.configExt.assetSelectionHelper.ensureGpsOptions(this.app, this.gpsEquipment);
    }
}
