import {EventEmitter} from "@angular/core";
import {UUID} from "angular2-uuid";

import {AppContext} from "app/app.service";
import {AssetGraphResponseExtended, AssetGraphTreeNode, SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {AssetExtended, ChartPointTupleProviderImpl, DeviceElementExtended, DeviceElementSchema, LocationExtended, MetricsDeviceElementExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {GraphContextUpdater} from "app/services/domain/dashboard-management.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import {EngineeringUnitsDescriptorExtended, UnitsService} from "app/services/domain/units.service";
import * as Models from "app/services/proxy/model/models";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";
import {ConditionNode, ConditionNodeType} from "app/shared/assets/tag-condition-builder/tag-conditions";
import {HierarchicalVisualizationComponent} from "app/shared/charting/hierarchical-visualization/hierarchical-visualization.component";
import {ScatterPlotContainerComponent} from "app/shared/charting/scatter-plot/scatter-plot-container.component";
import {TimeSeriesChartComponent} from "app/shared/charting/time-series-chart/time-series-chart.component";
import {TimeSeriesChartingComponent} from "app/shared/charting/time-series-container/common";
import {TimeSeriesContainerComponent} from "app/shared/charting/time-series-container/time-series-container.component";
import {TimeSeriesDownloaderElement} from "app/shared/charting/time-series-downloader";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";
import {TimeDurationExtended} from "app/shared/forms/time-range/time-duration-extended";
import {GpsMapComponent} from "app/shared/mapping/gps-map/gps-map.component";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {CanvasZoneSelection} from "framework/ui/charting/app-charting-utilities";
import {ChartValueRange} from "framework/ui/charting/core/basics";
import {ChartColorUtilities, ColorGradientStop, PaletteId} from "framework/ui/charting/core/colors";
import {ChartPointSource, DataSourceTuple, PlaceHolderSource, ScatterPlotPoint, ScatterPlotPropertyTuple, ScatterPlotSubSource, VisualizationDataSourceState} from "framework/ui/charting/core/data-sources";
import {TimeRangeId} from "framework/ui/charting/core/time";
import {ControlOption} from "framework/ui/control-option";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {filterAsync, Future, inParallel, mapInParallel, mapInParallelNoNulls} from "framework/utils/concurrency";
import {Debouncer} from "framework/utils/debouncers";
import {Memoizer, ResetMemoizers} from "framework/utils/memoizers";
import moment from "framework/utils/moment";

import {Subject} from "rxjs";

export class ControlPointMetadata
{
    constructor(public readonly id: string,
                public readonly point: DeviceElementExtended,
                private m_parent: AssetExtended,
                private m_equip: AssetExtended,
                private m_location: LocationExtended,
                private m_locationFullName: string)
    {}

    static async fromId(app: AppContext,
                        id: string): Promise<ControlPointMetadata>
    {
        let point = await app.domain.assets.getTypedExtendedById(DeviceElementExtended, id);
        if (!point) return null;

        let parentPromise    = point.getParent();
        let equipmentPromise = point.getEquipment();
        let locationPromise  = point.getLocation();

        let parent    = await parentPromise;
        let equipment = await equipmentPromise;
        let location  = await locationPromise;

        let locationFullName = location ? await location.getRecursiveName() : null;
        return new ControlPointMetadata(id, point, parent, equipment, location, locationFullName);
    }

    get name(): string
    {
        return this.point.typedModel?.name;
    }

    get physicalName(): string
    {
        return this.point.typedModel?.physicalName;
    }

    get logicalName(): string
    {
        return this.point.typedModel?.logicalName;
    }

    get parentName(): string
    {
        return this.m_parent?.typedModel?.name;
    }

    get locationName(): string
    {
        return this.m_location?.model?.name;
    }

    get fullLocationName(): string
    {
        return this.m_locationFullName;
    }

    standardDescription(): string
    {
        // Build a simple description of the source
        let description = [];
        if (this.m_equip) description.push(this.m_equip.model.name);
        if (this.parentName) description.push(this.parentName);
        if (this.fullLocationName) description.push(this.fullLocationName);
        return description.join(" / ");
    }
}

export class TimeSeriesSourceHost
{
    constructor(public readonly comp: SharedSvc.BaseApplicationComponent)
    {
    }

    get app(): AppContext
    {
        return this.comp.app;
    }
}

export class TimeSeriesSourceParameters
{
    property: string;
    units: Models.EngineeringUnitsFactors;

    rangeStart: moment.Moment;
    rangeEnd: moment.Moment;
    timeOffset: Models.TimeDuration;

    includeAlerts?: boolean;

    showMovingAverage?: number;
    onlyShowMovingAverage?: boolean;

    //--//

    static isCompatible(current: TimeSeriesSourceParameters,
                        next: TimeSeriesSourceParameters,
                        allowStartAdvance: boolean): boolean
    {
        if (UtilsService.compareJson(current, next)) return true;

        if (allowStartAdvance && current.rangeStart.valueOf() < next.rangeStart.valueOf())
        {
            let copyCurrent = {...current};
            let copyNext    = {...next};

            copyCurrent.rangeStart = undefined;
            copyNext.rangeStart    = undefined;

            if (UtilsService.compareJson(copyCurrent, copyNext)) return true;
        }

        return false;
    }
}

export class TimeSeriesSource
{
    get id(): string
    {
        return this.meta?.id;
    }

    constructor(public host: TimeSeriesSourceHost,
                public unitsService: UnitsService,
                public meta: ControlPointMetadata,
                public schema: DeviceElementSchema)
    {}

    public static async sourceFromId(host: TimeSeriesSourceHost,
                                     id: string): Promise<TimeSeriesSource>
    {
        let meta = await ControlPointMetadata.fromId(host.app, id);
        if (!meta) return null;

        return new TimeSeriesSource(host, host.app.domain.units, meta, await meta.point.fetchSchema());
    }

    public static async sourceExtFromId(app: AppContext,
                                        id: string,
                                        meta?: ControlPointMetadata,
                                        dimension?: string): Promise<TimeSeriesSourceConfigurationExtended>
    {
        if (!meta)
        {
            meta = await ControlPointMetadata.fromId(app, id);
            if (!meta) return null;
        }

        let model = TimeSeriesSourceConfigurationExtended.newModel(Models.TimeSeriesSourceConfiguration.newInstance({
                                                                                                                        id       : id,
                                                                                                                        dimension: dimension
                                                                                                                    }));
        return TimeSeriesSourceConfigurationExtended.newInstance(model, null, meta, null);
    }

    async generateExtended(dimension: string): Promise<TimeSeriesSourceConfigurationExtended>
    {
        return TimeSeriesSource.sourceExtFromId(this.host.app, this.meta.id, this.meta, dimension || DeviceElementExtended.PRESENT_VALUE);
    }

    getDataSource(parameters: TimeSeriesSourceParameters): Promise<ChartPointSource<any>>
    {
        return this.meta.point.getDataSourceForProperty(this.host.comp, parameters);
    }

    async currentUnits(dimension: string): Promise<Models.EngineeringUnitsFactors>
    {
        let schema = await this.meta.point.getSchemaProperty(dimension);
        return schema?.unitsFactors;
    }

    getDimensionOptions(): ControlOption<string>[]
    {
        let dimensions = Object.keys(this.schema);
        let options    = [];

        for (let d of dimensions) options.push(new ControlOption(d, this.schema[d].displayName));

        return options;
    }
}

export abstract class TimeSeriesChartHandler
{
    protected constructor(protected readonly chartExt: TimeSeriesChartConfigurationExtended)
    {
    }

    public static newInstance(chartExt: TimeSeriesChartConfigurationExtended): TimeSeriesChartHandler
    {
        switch (chartExt.model.type)
        {
            case Models.TimeSeriesChartType.STANDARD:
            case Models.TimeSeriesChartType.GRAPH:
                return new LineChartHandler(chartExt);

            case Models.TimeSeriesChartType.COORDINATE:
                return new GpsPathChartHandler(chartExt);

            case Models.TimeSeriesChartType.SCATTER:
            case Models.TimeSeriesChartType.GRAPH_SCATTER:
                return new ScatterChartHandler(chartExt);

            case Models.TimeSeriesChartType.HIERARCHICAL:
                return new HierarchicalChartHandler(chartExt);

            default:
                return null;
        }
    }

    abstract hasSources(): boolean;

    abstract showSourcePills(): boolean;

    abstract consolidatedChipTooltip(): string;

    abstract dataSourceType(): string;

    abstract editableColors(): boolean;

    abstract exportablePng(): boolean;

    abstract getChartingElement(containerComponent: TimeSeriesContainerComponent): TimeSeriesChartingComponent;

    abstract getInteractableChart(containerComponent: TimeSeriesContainerComponent): InteractableSourcesChart;

    abstract withPadding(isEmbedded: boolean): boolean

    abstract getDownloaderElements(app: AppContext): Promise<TimeSeriesDownloaderElement[]>;
}

class LineChartHandler extends TimeSeriesChartHandler
{
    hasSources(): boolean
    {
        return this.chartExt.sourcesExt.length > 0;
    }

    showSourcePills(): boolean
    {
        return !this.chartExt.model.display.hideSources;
    }

    consolidatedChipTooltip(): string
    {
        return "View all sources";
    }

    dataSourceType(): string
    {
        return "Data Source";
    }

    editableColors(): boolean
    {
        return false;
    }

    exportablePng(): boolean
    {
        return true;
    }

    getChartingElement(containerComponent: TimeSeriesContainerComponent): TimeSeriesChartComponent
    {
        return containerComponent.chartElement;
    }

    getInteractableChart(containerComponent: TimeSeriesContainerComponent): InteractableSourcesChart
    {
        return this.getChartingElement(containerComponent);
    }

    withPadding(isEmbedded: boolean): boolean
    {
        return !isEmbedded;
    }

    async getDownloaderElements(app: AppContext): Promise<TimeSeriesDownloaderElement[]>
    {
        return mapInParallelNoNulls(this.chartExt.dataSources, async (source) =>
        {
            let meta = await ControlPointMetadata.fromId(app, source.id);
            if (!meta) return null;

            return {
                name   : `${meta.name} - ${meta.standardDescription()}`,
                element: meta.point
            };
        });
    }
}

class GpsPathChartHandler extends TimeSeriesChartHandler
{
    hasSources(): boolean
    {
        return this.chartExt.model.dataSources.length > 0;
    }

    showSourcePills(): boolean
    {
        return this.chartExt.mapSources?.length > 1;
    }

    consolidatedChipTooltip(): string
    {
        return "View all GPS";
    }

    dataSourceType(): string
    {
        return "GPS Asset";
    }

    editableColors(): boolean
    {
        return this.showSourcePills();
    }

    exportablePng(): boolean
    {
        return true;
    }

    getChartingElement(containerComponent: TimeSeriesContainerComponent): GpsMapComponent
    {
        return containerComponent.mapElement;
    }

    getInteractableChart(containerComponent: TimeSeriesContainerComponent): InteractableSourcesChart
    {
        return this.getChartingElement(containerComponent);
    }

    withPadding(isEmbedded: boolean): boolean
    {
        return true;
    }

    async getDownloaderElements(): Promise<TimeSeriesDownloaderElement[]>
    {
        return null;
    }
}

class ScatterChartHandler extends TimeSeriesChartHandler
{
    hasSources(): boolean
    {
        return this.chartExt.model.scatterPlot.sourceTuples.length > 0;
    }

    showSourcePills(): boolean
    {
        return true;
    }

    consolidatedChipTooltip(): string
    {
        return "View all tuples";
    }

    dataSourceType(): string
    {
        return "Source Tuple";
    }

    editableColors(): boolean
    {
        return true;
    }

    exportablePng(): boolean
    {
        return true;
    }

    getChartingElement(containerComponent: TimeSeriesContainerComponent): ScatterPlotContainerComponent
    {
        return containerComponent.scatterElement;
    }

    getInteractableChart(containerComponent: TimeSeriesContainerComponent): InteractableSourcesChart
    {
        return this.getChartingElement(containerComponent);
    }

    withPadding(isEmbedded: boolean): boolean
    {
        return false;
    }

    async getDownloaderElements(): Promise<TimeSeriesDownloaderElement[]>
    {
        return null;
    }
}

class HierarchicalChartHandler extends TimeSeriesChartHandler
{
    hasSources(): boolean
    {
        return this.chartExt.hierarchicalGraph && !!this.chartExt.model.hierarchy.bindings?.length;
    }

    showSourcePills(): boolean
    {
        return false;
    }

    consolidatedChipTooltip(): string
    {
        return null;
    }

    dataSourceType(): string
    {
        return null;
    }

    editableColors(): boolean
    {
        return true;
    }

    exportablePng(): boolean
    {
        return false;
    }

    getChartingElement(containerComponent: TimeSeriesContainerComponent): HierarchicalVisualizationComponent
    {
        return containerComponent.hierarchyElement;
    }

    getInteractableChart(): InteractableSourcesChart
    {
        return null;
    }

    withPadding(isEmbedded: boolean): boolean
    {
        return true;
    }

    async getDownloaderElements(app: AppContext): Promise<TimeSeriesDownloaderElement[]>
    {
        const graphExt      = new SharedAssetGraphExtended(app.domain, SharedAssetGraphExtended.newModel(this.chartExt.hierarchicalGraph, null, null));
        const response      = await graphExt.resolve();
        const controlPoints = await response.resolveControlPoints(app.domain, [Models.AssetGraphBinding.newInstance({nodeId: this.chartExt.model.hierarchy.bindings[0].leafNodeId})]);

        return mapInParallelNoNulls(controlPoints, async (controlPoint) =>
        {
            let meta = await ControlPointMetadata.fromId(app, controlPoint.sysId);
            if (!meta) return null;

            return {
                name   : `${meta.name} - ${meta.standardDescription()}`,
                element: meta.point
            };
        });
    }
}

export class TimeSeriesChartConfigurationExtended implements GraphConfigurationHost
{
    public readonly id: string;

    public readonly hostContext = "Chart";

    public readonly assetSelectionHelper = new AssetSelectionHelper();

    get chartHandler(): TimeSeriesChartHandler
    {
        return TimeSeriesChartHandler.newInstance(this);
    }

    private m_mapSourcesLoaded: Future<void>;
    private m_mapSources: AssetExtended[];
    get mapSources(): AssetExtended[]
    {
        if (!this.m_mapSources) this.setMapSources();

        return this.m_mapSources;
    }

    get dataSources(): Models.TimeSeriesSourceConfiguration[]
    {
        if (!this.model.dataSources)
        {
            this.model.dataSources = [];
        }

        return this.model.dataSources;
    }

    get hierarchicalGraph(): Models.AssetGraph
    {
        return this.model.graph?.sharedGraphs?.[0]?.graph;
    }

    get hasExternalBindings(): boolean
    {
        return !!this.model.graph?.externalBindings?.length;
    }

    public static hasDataSources(m: Models.TimeSeriesChartConfiguration)
    {
        return m?.dataSources?.length > 0;
    }

    private m_resolvedWithExternal: boolean = false;
    private m_resolvedGraphs                = new Map<string, SharedAssetGraphExtended>();

    public readonly panelsExt: TimeSeriesPanelConfigurationExtended[]   = [];
    public readonly sourcesExt: TimeSeriesSourceConfigurationExtended[] = [];
    public annotationsExt: TimeSeriesAnnotationConfigurationExtended[]  = [];

    private readonly m_identifierToSource: Lookup<TimeSeriesSourceConfigurationExtended> = {};
    private readonly m_idToSources: Lookup<TimeSeriesSourceConfigurationExtended[]>      = {};

    public configChanged   = new Subject<boolean>();
    public mapSourcesReady = new Subject<void>();
    public graphsChanged   = new Subject<void>();

    externalGraph: Models.TimeSeriesGraphConfiguration;
    externalContextUpdaters: GraphContextUpdater[];

    private constructor(public readonly app: AppContext,
                        public readonly model: Models.TimeSeriesChartConfiguration = TimeSeriesChartConfigurationExtended.newModel(),
                        sourceChart?: TimeSeriesChartConfigurationExtended,
                        id?: string)
    {
        // backwards compatibility
        if (!this.model.annotations) this.model.annotations = [];

        this.id = id || sourceChart?.id || UUID.UUID();

        for (let panel of this.model.panels || [])
        {
            let panelExt = TimeSeriesPanelConfigurationExtended.newInstance(this, panel);

            panelExt.index = this.panelsExt.length;
            this.panelsExt.push(panelExt);
        }

        for (let sourceExt of sourceChart?.sourcesExt || [])
        {
            this.addSourceExt(sourceExt);
        }

        for (let annotation of this.model.annotations)
        {
            let panelExt = this.panelsExt[annotation.panel];
            if (panelExt) this.annotationsExt.push(new TimeSeriesAnnotationConfigurationExtended(annotation, panelExt));
        }
        this.validateAnnotations();
    }

    async setMapSources(sourcesChanged?: boolean): Promise<void>
    {
        const sources = this.dataSources;
        const isValid = this.model.type === Models.TimeSeriesChartType.COORDINATE && sources.length === this.sourcesExt.length;
        const ready   = !this.m_mapSourcesLoaded && (!this.m_mapSources || sourcesChanged);
        if (isValid && ready)
        {
            this.m_mapSourcesLoaded = new Future();
            this.m_mapSources       = [];

            let recordIds      = sources.map((source) => DeviceElementExtended.newIdentity(source.id));
            let deviceElemsExt = await this.app.domain.assets.getTypedExtendedBatch(DeviceElementExtended, recordIds);
            this.m_mapSources  = await mapInParallelNoNulls(deviceElemsExt, (deviceElemExt,
                                                                             idx) => this.sourcesExt[idx]?.loadMapSource(deviceElemExt));

            this.m_mapSourcesLoaded.resolve();
            this.mapSourcesReady.next();

            this.m_mapSourcesLoaded = null;
        }

        return this.m_mapSourcesLoaded;
    }

    async loadExternalCoordinateId(): Promise<string>
    {
        let externalContexts = this.externalGraph?.contexts;
        if (this.model.type === Models.TimeSeriesChartType.COORDINATE && this.hasExternalBindings && externalContexts)
        {
            let externalBinding = this.model.graph.externalBindings[0];
            let graphLookup     = await this.resolveGraphs(true);
            let graphExt        = graphLookup.get(externalBinding.graphId);
            let responseHolder  = await graphExt.resolveWithContext(externalContexts);
            let gpsIdentity     = await responseHolder.responses[0]?.resolveInputIdentity(externalBinding);
            if (gpsIdentity?.sysId)
            {
                let newSources = await this.getCoordinateDataSourceIds([gpsIdentity.sysId]);
                let latitudeId = newSources[0];
                if (latitudeId) return latitudeId;
            }
        }
        return null;
    }

    public static constructFrom(chartExt: TimeSeriesChartConfigurationExtended): TimeSeriesChartConfigurationExtended
    {
        if (!chartExt) return null;

        const newConfigExt         = new TimeSeriesChartConfigurationExtended(chartExt.app, chartExt.model, chartExt);
        newConfigExt.externalGraph = chartExt.externalGraph;

        for (let i = 0; i < newConfigExt.sourcesExt.length; i++)
        {
            const sourceNew = newConfigExt.sourcesExt[i];
            const sourceOld = chartExt.sourcesExt[i];
            sourceNew.attemptAdoptCachedData(sourceOld);
        }
        newConfigExt.setMapSources();

        return newConfigExt;
    }

    public static newModel(): Models.TimeSeriesChartConfiguration
    {
        return Models.TimeSeriesChartConfiguration.newInstance({
                                                                   type       : Models.TimeSeriesChartType.STANDARD,
                                                                   display    : TimeSeriesDisplayConfigurationExtended.newModel(),
                                                                   panels     : [],
                                                                   dataSources: [],
                                                                   hierarchy  : this.emptyHierarchy(),
                                                                   scatterPlot: Models.ScatterPlot.newInstance({sourceTuples: []}),
                                                                   palette    : ChartColorUtilities.defaultPalette,
                                                                   annotations: []
                                                               });
    }

    public static emptyHierarchy(): Models.HierarchicalVisualization
    {
        return Models.HierarchicalVisualization.newInstance({
                                                                bindings    : [],
                                                                virtualNodes: []
                                                            });
    }

    public static defaultHierarchicalOptions(): Models.HierarchicalVisualizationConfiguration
    {
        return Models.HierarchicalVisualizationConfiguration.newInstance({
                                                                             sizing    : Models.HierarchicalVisualizationSizing.FIT,
                                                                             size      : 32,
                                                                             type      : Models.HierarchicalVisualizationType.LINE,
                                                                             axisSizing: Models.HierarchicalVisualizationAxisSizing.INDIVIDUAL,
                                                                             axisRange : Models.NumericRange.newInstance({
                                                                                                                             min: 0,
                                                                                                                             max: 100
                                                                                                                         })
                                                                         });
    }

    public static emptyInstance(app: AppContext): TimeSeriesChartConfigurationExtended
    {
        return new TimeSeriesChartConfigurationExtended(app);
    }

    public static async newInstance(app: AppContext,
                                    model?: Models.TimeSeriesChartConfiguration,
                                    id?: string,
                                    externalGraph?: Models.TimeSeriesGraphConfiguration): Promise<TimeSeriesChartConfigurationExtended>
    {
        let chartExt = new TimeSeriesChartConfigurationExtended(app, model, undefined, id);
        if (externalGraph) chartExt.externalGraph = externalGraph;

        let ids                                        = new Set<string>(chartExt.dataSources.map((source) => source.id));
        let idToMetadata: Lookup<ControlPointMetadata> = {};
        await inParallel([...ids], async (id) => idToMetadata[id] = await ControlPointMetadata.fromId(chartExt.app, id));

        switch (chartExt.model.type)
        {
            case Models.TimeSeriesChartType.GRAPH:
            case Models.TimeSeriesChartType.STANDARD:
                let sourceModifications = await chartExt.standardGraphSourceRebuild(DeviceElementExtended.PRESENT_VALUE);

                await inParallel(chartExt.dataSources, async (dataSource) =>
                {
                    if (dataSource.pointBinding) return null;
                    let sourceExt = await TimeSeriesSourceConfigurationExtended.newInstance(dataSource, null, idToMetadata[dataSource.id], null);
                    sourceModifications.adds.push(sourceExt);
                });

                chartExt.model.dataSources = [];
                await chartExt.applySourceChanges(sourceModifications.adds, []);
                break;

            case Models.TimeSeriesChartType.COORDINATE:
                if (chartExt.hasExternalBindings && chartExt.externalGraph?.contexts.length)
                {
                    let coordinateId = await chartExt.loadExternalCoordinateId();
                    if (coordinateId)
                    {
                        let meta                   = idToMetadata[coordinateId] || await ControlPointMetadata.fromId(chartExt.app, coordinateId);
                        let sourceExt              = await chartExt.buildCoordinateSourceExt(meta, null, null);
                        chartExt.model.dataSources = [sourceExt.model];
                        chartExt.addSourceExt(sourceExt);
                    }
                }
                else
                {
                    let newSourceExts = await mapInParallel(chartExt.dataSources, (dataSource) => chartExt.buildCoordinateSourceExt(idToMetadata[dataSource.id], dataSource, null));
                    for (let sourceExt of newSourceExts) chartExt.addSourceExt(sourceExt);
                }

                await chartExt.setMapSources();
                break;
        }

        return chartExt;
    }

    public static async generateNewInstanceFromSources(app: AppContext,
                                                       mainExt: TimeSeriesSourceConfigurationExtended,
                                                       secondaryExt: TimeSeriesSourceConfigurationExtended): Promise<TimeSeriesChartConfigurationExtended>
    {
        let extended = TimeSeriesChartConfigurationExtended.emptyInstance(app);

        if (!mainExt)
        {
            mainExt      = secondaryExt;
            secondaryExt = null;
        }

        let sourcesExt = [];
        let useDimensionAsLabel: boolean;
        if (mainExt)
        {
            extended.addNewPanel();
            sourcesExt.push(mainExt);

            let first           = mainExt.model;
            first.color         = "blue";
            useDimensionAsLabel = !mainExt.name;

            if (secondaryExt)
            {
                extended.addNewPanel();
                sourcesExt.push(secondaryExt);

                let second          = secondaryExt.model;
                second.color        = "green";
                second.panel        = 1;
                useDimensionAsLabel = (!mainExt.name || !secondaryExt.name) || first.id === second.id;
            }
        }
        await extended.applySourceChanges(sourcesExt, [], true);

        for (let sourceExt of sourcesExt)
        {
            let panelExt = sourceExt?.ownerPanel;
            if (panelExt)
            {
                let source   = sourceExt.model;
                let leftAxis = panelExt.leftAxisExtended.model;

                leftAxis.label = useDimensionAsLabel ? source.dimension : sourceExt.name;
                leftAxis.color = source.color;
            }
        }

        return extended;
    }

    public static cleanForComparison(chart: Models.TimeSeriesChartConfiguration,
                                     ignoreAxes: boolean,
                                     ignoreAnnotations: boolean): Models.TimeSeriesChartConfiguration
    {
        if (!chart) return null;

        chart = Models.TimeSeriesChartConfiguration.deepClone(chart);

        if (chart.hierarchy)
        {
            for (let binding of chart.hierarchy.bindings)
            {
                if (binding.options.type === Models.HierarchicalVisualizationType.LINE)
                {
                    binding.color.segments = null;
                }
                else
                {
                    binding.color.paletteName = null;
                }
            }
        }

        for (let source of chart.dataSources || [])
        {
            ToggleableNumericRangeExtended.cleanModel(source.range);
        }

        if (ignoreAxes)
        {
            for (let panel of chart.panels)
            {
                panel.leftAxis  = null;
                panel.rightAxis = null;
                panel.xAxis     = null;
            }
        }
        else
        {
            if (chart.type !== Models.TimeSeriesChartType.SCATTER && chart.type !== Models.TimeSeriesChartType.GRAPH_SCATTER)
            {
                for (let panel of chart.panels) panel.xAxis = null;
            }
            else
            {
                for (let panel of chart.panels)
                {
                    panel.leftAxis.groupedFactors  = null;
                    panel.xAxis.groupedFactors     = null;
                    panel.rightAxis.groupedFactors = null;
                }
            }

            for (let panel of chart.panels)
            {
                ToggleableNumericRangeExtended.cleanModel(panel.leftAxis.override);
                ToggleableNumericRangeExtended.cleanModel(panel.rightAxis.override);
                if (panel.xAxis) ToggleableNumericRangeExtended.cleanModel(panel.xAxis.override);
            }
        }

        if (ignoreAnnotations)
        {
            chart.annotations = null;
        }

        return chart;
    }

    public static getScatterTupleIndices(sourceId: string): number[]
    {
        let commaIndex = sourceId.indexOf(",");
        return [
            parseInt(sourceId.substring(0, commaIndex)),
            parseInt(sourceId.substring(commaIndex + 1))
        ];
    }

    public validateAnnotations()
    {
        this.annotationsExt    = this.annotationsExt.filter((annotation) => annotation.valid);
        this.model.annotations = this.annotationsExt.map((annotation) => annotation.model);
    }

    public buildCoordinateSourceExt(meta: ControlPointMetadata,
                                    model: Models.TimeSeriesSourceConfiguration,
                                    dimension: string): Promise<TimeSeriesSourceConfigurationExtended>
    {
        if (meta)
        {
            if (!model || model.id !== meta.id)
            {
                model = TimeSeriesSourceConfigurationExtended.newModel(Models.TimeSeriesSourceConfiguration.newInstance({
                                                                                                                            id       : meta.id,
                                                                                                                            dimension: dimension || DeviceElementExtended.PRESENT_VALUE
                                                                                                                        }));
            }

            return TimeSeriesSourceConfigurationExtended.newInstance(model, this, meta, null);
        }

        return null;
    }

    public adoptExternalGraphs()
    {
        if (this.externalGraph)
        {
            this.initializeGraphs();

            let externalToInternalId: Lookup<string> = {};
            for (let externalGraph of this.externalGraph.sharedGraphs || [])
            {
                externalToInternalId[externalGraph.id] = UUID.UUID();
                this.model.graph.sharedGraphs.push(SharedAssetGraphExtended.newModel(externalGraph.graph, externalToInternalId[externalGraph.id], externalGraph.name));
            }

            for (let externalContext of this.externalGraph.contexts)
            {
                this.model.graph.contexts.push(Models.AssetGraphContext.newInstance(
                    {
                        ...externalContext,
                        graphId: externalToInternalId[externalContext.graphId]
                    }
                ));
            }

            for (let source of this.model.dataSources)
            {
                let binding = source.pointBinding;
                if (binding)
                {
                    let internalId = externalToInternalId[binding.graphId];
                    if (internalId) binding.graphId = internalId;
                }
            }

            this.model.graph.externalBindings = [];
            this.externalGraph                = null;
        }
    }

    public async updateExternalGraph(graph: Models.TimeSeriesGraphConfiguration)
    {
        if (graph)
        {
            this.externalGraph = graph;

            await this.resolveGraphs(true);
            if (this.model.type === Models.TimeSeriesChartType.COORDINATE)
            {
                let coordinateId = await this.loadExternalCoordinateId();
                if (coordinateId) await this.changeGpsSources([coordinateId]);
            }
            else
            {
                await this.applyStandardGraphSourceChanges(this.standardGraphBindingSet());
            }
        }
    }

    public adoptSourceData(otherChartExt: TimeSeriesChartConfigurationExtended)
    {
        if (!otherChartExt?.sourcesExt) return;

        let currSourceExts: Lookup<TimeSeriesSourceConfigurationExtended> = {};
        for (let sourceExt of this.sourcesExt) currSourceExts[sourceExt.identifier] = sourceExt;

        for (let otherSourceExt of otherChartExt.sourcesExt)
        {
            let currSourceExt = currSourceExts[otherSourceExt.identifier];
            if (currSourceExt) currSourceExt.attemptAdoptCachedData(otherSourceExt);
        }
    }

    public getSourcesStates(chart: InteractableSourcesChart): Lookup<VisualizationDataSourceState>
    {
        let stateMap: Lookup<VisualizationDataSourceState> = {};
        switch (this.model.type)
        {
            case Models.TimeSeriesChartType.STANDARD:
            case Models.TimeSeriesChartType.GRAPH:
            case Models.TimeSeriesChartType.COORDINATE:
                for (let sourceExt of this.sourcesExt)
                {
                    let identifier       = sourceExt.identifier;
                    stateMap[identifier] = chart.getSourceState(identifier);
                }
                break;

            case Models.TimeSeriesChartType.SCATTER:
            case Models.TimeSeriesChartType.GRAPH_SCATTER:
                let scatterChart = <ScatterPlotContainerComponent>chart;
                for (let panelTupleIds of scatterChart.tupleIdsByPanel())
                {
                    for (let tupleId of panelTupleIds) stateMap[tupleId] = chart.getSourceState(tupleId);
                }
                break;
        }

        return stateMap;
    }

    public addNewPanel(): TimeSeriesPanelConfigurationExtended
    {
        let ext = TimeSeriesPanelConfigurationExtended.newInstance(this);

        ext.index = this.model.panels.length;
        this.model.panels.push(ext.model);

        this.panelsExt.push(ext);

        return ext;
    }

    public getLastPanelExtended(buildIfUnavailable: boolean = false): TimeSeriesPanelConfigurationExtended
    {
        let index = this.panelsExt.length - 1;
        let panel = index >= 0 ? this.panelsExt[index] : null;
        if (!panel && buildIfUnavailable) panel = this.addNewPanel();
        return panel;
    }

    //--//

    public getSourcesById(id: string): TimeSeriesSourceConfigurationExtended[]
    {
        return this.m_idToSources[id];
    }

    public getSourceByIdentifier(identifier: string): TimeSeriesSourceConfigurationExtended
    {
        return this.m_identifierToSource[identifier];
    }

    public addSource(source: TimeSeriesSourceConfigurationExtended)
    {
        if (!source) return;

        this.dataSources.push(source.model);
        this.addSourceExt(source);
    }

    private addSourceExt(sourceExt: TimeSeriesSourceConfigurationExtended): void
    {
        if (!sourceExt || sourceExt.deleted) return;

        this.sourcesExt.push(sourceExt);

        let id          = sourceExt.model.id;
        let idToSources = this.m_idToSources[id];
        if (!idToSources)
        {
            idToSources            = [];
            this.m_idToSources[id] = idToSources;
        }
        idToSources.push(sourceExt);

        this.m_identifierToSource[sourceExt.identifier] = sourceExt;

        sourceExt.onDelete.then(() => this.applySourceChanges([], [sourceExt], undefined, true));

        let panelExt = this.panelsExt[sourceExt.model.panel];
        if (panelExt) panelExt.addSource(sourceExt);
    }

    private async addNewSource(source: TimeSeriesSourceConfigurationExtended)
    {
        let panel: TimeSeriesPanelConfigurationExtended;
        if (!isNaN(source.model.panel) && this.panelsExt.length > source.model.panel)
        {
            panel = this.panelsExt[source.model.panel];
        }
        else
        {
            // Get the last available panel to add to
            panel = this.getLastPanelExtended(true);

            // Assign the panel to the sources
            source.model.panel = panel.index;
        }

        this.addSource(source);

        await panel.addNewSource(this.app, this, source);
    }

    private removeSourceExt(source: TimeSeriesSourceConfigurationExtended)
    {
        let identifier = source.identifier;
        let pos        = this.sourcesExt.findIndex((modelExt) => modelExt.identifier === identifier);
        if (pos >= 0)
        {
            if (source.markedForDeletion)
            {
                source.clearDeleter();
            }
            else
            {
                let sourceIdx = this.dataSources.findIndex((s) => s.uuid === source.model.uuid && s.id === source.model.id);
                if (sourceIdx >= 0)
                {
                    this.dataSources.splice(sourceIdx, 1);
                }
            }

            let sourceExt       = this.sourcesExt.splice(pos, 1)[0];
            let sourcesOfSameId = this.m_idToSources[source.model.id];
            if (sourcesOfSameId.length > 1)
            {
                let internalPos = sourcesOfSameId.findIndex((sourceOfSameId) => source.model === sourceOfSameId.model);
                sourcesOfSameId.splice(internalPos, 1);
            }
            else
            {
                delete this.m_idToSources[source.model.id];
            }
            delete this.m_identifierToSource[identifier];

            let panel = sourceExt.ownerPanel;
            if (panel) panel.removeSource(source);
        }
    }

    private async removeOldSource(units: UnitsService,
                                  source: TimeSeriesSourceConfigurationExtended)
    {
        let panelExt = source.ownerPanel;

        this.removeSourceExt(source);

        if (panelExt)
        {
            // Is the panel empty?
            if (panelExt.sources.length == 0)
            {
                this.model.panels.splice(panelExt.index, 1);
                this.panelsExt.splice(panelExt.index, 1);

                panelExt.setIndex(-2);

                for (let i = 0; i < this.panelsExt.length; i++)
                {
                    this.panelsExt[i].setIndex(i);
                }
            }
            else
            {
                await panelExt.cleanOldSourceAxis(units, source);
            }
        }
    }

    public standardGraphBindingSet(): Models.AssetGraphBinding[]
    {
        let externalBindings                       = this.model.graph.externalBindings || [];
        let uniques                                = new Set<string>();
        const bindings: Models.AssetGraphBinding[] = [];

        for (let source of this.dataSources)
        {
            if (source.pointBinding && externalBindings.every((binding) => binding.graphId !== source.pointBinding.graphId))
            {
                const key = AssetGraphTreeNode.getIdFromBinding(source.pointBinding);
                if (!uniques.has(key))
                {
                    uniques.add(key);
                    bindings.push(AssetGraphTreeNode.getBinding(key));
                }
            }
        }

        for (let externalBinding of externalBindings)
        {
            bindings.push(Models.AssetGraphBinding.newInstance(externalBinding));
        }

        return bindings;
    }

    public canRemove(graphId: string): boolean
    {
        return true;
    }

    public canRemoveNode(graphId: string,
                         nodeId: string): boolean
    {
        return true;
    }

    private initializeGraphs()
    {
        if (!this.model.graph)
        {
            this.model.graph = new Models.TimeSeriesGraphConfiguration();
        }

        if (!this.model.graph.sharedGraphs)
        {
            this.model.graph.sharedGraphs = [];
        }

        if (!this.model.graph.contexts)
        {
            this.model.graph.contexts = [];
        }

        if (!this.model.graph.externalBindings)
        {
            this.model.graph.externalBindings = [];
        }
    }

    public getGraphs(includeExternal?: boolean): Models.SharedAssetGraph[]
    {
        this.initializeGraphs();

        if (includeExternal)
        {
            return this.model.graph.sharedGraphs.concat(this.externalGraph?.sharedGraphs || []);
        }
        return this.model.graph.sharedGraphs;
    }

    @ResetMemoizers
    public async resolveGraphs(includeExternal?: boolean): Promise<Map<string, SharedAssetGraphExtended>>
    {
        this.m_resolvedWithExternal = !!includeExternal;
        this.m_resolvedGraphs       = await SharedAssetGraphExtended.loadGraphs(this.app.domain, this.getGraphs(includeExternal));
        return this.m_resolvedGraphs;
    }

    public async loadGraphs()
    {
        const missingExternal = this.model.type !== Models.TimeSeriesChartType.HIERARCHICAL && !this.m_resolvedWithExternal;
        if (!this.m_resolvedGraphs.size || missingExternal)
        {
            await this.resolveGraphs(true);
        }
    }

    public get resolvedGraphs(): Map<string, SharedAssetGraphExtended>
    {
        return this.m_resolvedGraphs;
    }

    @Memoizer
    public async getGraphControlOptions(): Promise<Map<Models.AssetGraphBinding, ControlOption<string>[]>>
    {
        const result = new Map<Models.AssetGraphBinding, ControlOption<string>[]>();

        await this.loadGraphs();
        let resolvedGraphs = this.resolvedGraphs;
        let localGraphs    = new Map<string, SharedAssetGraphExtended>();
        for (let localGraph of this.getGraphs())
        {
            let localGraphId = localGraph.id;
            let graph        = resolvedGraphs.get(localGraphId);
            if (graph) localGraphs.set(localGraphId, graph);
        }

        for (const [graphId, graph] of localGraphs.entries())
        {
            const resolved   = await graph.resolve();
            const allOptions = await resolved.getControlOptions();
            for (const [nodeId, options] of allOptions.entries())
            {
                result.set(Models.AssetGraphBinding.newInstance({
                                                                    graphId,
                                                                    nodeId
                                                                }), options);
            }
        }

        return result;
    }

    public async getCoordinateDataSourceIds(gpsIds: string[]): Promise<string[]>
    {
        if (!gpsIds?.length) return [];

        let filter           = Models.DeviceElementFilterRequest.newInstance({parentIDs: gpsIds});
        let listResponse     = await this.app.domain.assets.getList(filter);
        let deviceElemsToAdd = await this.app.domain.assets.getTypedPage(DeviceElementExtended, listResponse.results, 0, 1000);
        deviceElemsToAdd     = deviceElemsToAdd.filter((deviceElem) => deviceElem.typedModel.identifier === "latitude");
        return deviceElemsToAdd.map((deviceElem) => deviceElem.model.sysId);
    }

    public async applySourceChanges(additions: TimeSeriesSourceConfigurationExtended[],
                                    removals: TimeSeriesSourceConfigurationExtended[],
                                    allowDuplicates: boolean  = false,
                                    fromChipDeletion: boolean = false): Promise<SourceModificationResult>
    {
        let result = new SourceModificationResult();

        // Handle additions
        for (let addition of additions)
        {
            if (addition)
            {
                if (!allowDuplicates)
                {
                    let matchingSources = this.sourcesExt.filter((s) => s.model.id === addition.model.id);
                    const binding       = addition.model.pointBinding;
                    if (binding)
                    {
                        matchingSources = matchingSources.filter((s) =>
                                                                 {
                                                                     if (s.contextId !== addition.contextId) return false;

                                                                     let sP = s.model.pointBinding;
                                                                     if (sP?.graphId !== binding.graphId) return false;
                                                                     return sP.nodeId === binding.nodeId;
                                                                 });
                    }

                    if (matchingSources.length)
                    {
                        result.duplicates.push(addition);
                        continue;
                    }
                }

                // Only add if we have minimum viable data
                if (addition.hasMinimumData)
                {
                    result.added.push(addition);
                    await this.addNewSource(addition);
                }
                else
                {
                    result.ignored.push(addition);
                }
            }
        }

        // Handle removals
        for (let removal of removals)
        {
            if (removal)
            {
                if (!removal.ownerPanel || removal.ownerPanel.owner == this)
                {
                    result.removed.push(removal);
                    await this.removeOldSource(this.app.domain.units, removal);
                }
            }
        }

        this.validateAnnotations();

        for (let source of this.dataSources)
        {
            if (!source.color) source.color = this.nextBestSourceColor();
        }

        if (result.added.length > 0 || result.removed.length > 0)
        {
            this.configChanged.next(!fromChipDeletion);
        }

        // Return the changes
        return result;
    }

    //--//

    public async getAssetGraphResponses(): Promise<{ context: Models.AssetGraphContext, responses: AssetGraphResponseExtended[] }[]>
    {
        let contexts = (this.model.graph.contexts || []).concat(this.externalGraph?.contexts || []);
        let response = [];
        for (let context of contexts)
        {
            const graph = this.resolvedGraphs.get(context.graphId);
            if (!graph || !context.nodeId) continue;

            const available = await graph.resolve();
            if (context instanceof Models.AssetGraphContextAsset)
            {
                let matchedGraphs = await available.findResponsesByPrimaryIds(context.nodeId, [context.sysId]);
                response.push({
                                  context  : Models.AssetGraphContextAsset.newInstance(context),
                                  responses: matchedGraphs
                              });
            }
            else if (context instanceof Models.AssetGraphContextAssets)
            {
                let selectAll = context.selectAll;
                if (selectAll)
                {
                    response.push({
                                      context  : Models.AssetGraphContextAssets.newInstance(context),
                                      responses: available.responses
                                  });
                }
                else
                {
                    for (let sysId of context.sysIds)
                    {
                        let singularContext = Models.AssetGraphContextAsset.newInstance({
                                                                                            graphId: context.graphId,
                                                                                            nodeId : context.nodeId,
                                                                                            sysId  : sysId
                                                                                        });
                        let matchedGraphs   = await available.findResponsesByPrimaryIds(context.nodeId, [sysId]);
                        response.push({
                                          context  : singularContext,
                                          responses: matchedGraphs
                                      });
                    }
                }
            }
        }

        return response;
    }

    public async standardGraphSourceRebuild(dimension: string): Promise<SourceModificationRequest>
    {
        await this.loadGraphs();

        let graphSources = this.dataSources.filter((source) => source.pointBinding);

        let sourceGenerator = new class extends AssetGraphLineSourceGenerator
        {
            public getId(contextSysId: string,
                         nodeBinding: Models.AssetGraphBinding): string
            {
                return AssetGraphTreeNode.getIdFromBinding(nodeBinding);
            }

            public buildNewSource(matchingRecord: Models.RecordIdentity,
                                  nodeBinding: Models.AssetGraphBinding,
                                  matchingConfig: Models.TimeSeriesSourceConfiguration): Models.TimeSeriesSourceConfiguration
            {
                return Models.TimeSeriesSourceConfiguration.newInstance({
                                                                            ...(matchingConfig || {}),
                                                                            id          : matchingRecord?.sysId,
                                                                            pointBinding: nodeBinding
                                                                        });
            }
        }();

        for (let source of graphSources)
        {
            let bindingId = sourceGenerator.getId(undefined, source.pointBinding);
            sourceGenerator.pushConfig(bindingId, source);
        }

        let keepSources = await this.getStandardGraphSources(this.standardGraphBindingSet(), sourceGenerator, dimension);

        return this.getStandardGraphSourceModificationRequest(keepSources);
    }

    public async standardGraphSourceChanges(nodeBindings: Models.AssetGraphBinding[],
                                            dimension: string): Promise<SourceModificationRequest>
    {
        let contextResponses = await this.getAssetGraphResponses();

        let boundSourceExts = this.sourcesExt.filter((sourceExt) => sourceExt.contextId);
        let prevContextIds  = new Set(boundSourceExts.map((sourceExt) => sourceExt.contextId));
        let currContextIds  = new Set<string>();

        for (let entry of contextResponses)
        {
            if (entry.context instanceof Models.AssetGraphContextAsset)
            {
                currContextIds.add(entry.context.sysId);
            }
        }

        let res               = UtilsService.compareSets(prevContextIds, currContextIds);
        let removedContextIds = [...res.notInB];
        let newContexts       = res.notInA;

        let newToOldContext: Lookup<string> = {};
        let removedIdx                      = 0;
        for (let newContext of newContexts)
        {
            if (removedIdx === removedContextIds.length) break;
            newToOldContext[newContext] = removedContextIds[removedIdx++];
        }

        let sourceGenerator = new class extends AssetGraphLineSourceGenerator
        {
            public getId(contextSysId: string,
                         nodeBinding: Models.AssetGraphBinding): string
            {
                let nodeId = AssetGraphTreeNode.getIdFromBinding(nodeBinding);

                contextSysId = newToOldContext[contextSysId] || contextSysId;

                return contextSysId ? `${contextSysId}/${nodeId}` : nodeId;
            }

            public buildNewSource(matchingRecord: Models.RecordIdentity,
                                  nodeBinding: Models.AssetGraphBinding,
                                  matchingConfig: Models.TimeSeriesSourceConfiguration): Models.TimeSeriesSourceConfiguration
            {
                if (!matchingConfig)
                {
                    let matchingConfigTemplate = this.ensureTemplate(nodeBinding, undefined);

                    matchingConfig               = Models.TimeSeriesSourceConfiguration.deepClone(matchingConfigTemplate);
                    matchingConfigTemplate.color = null;
                }

                return Models.TimeSeriesSourceConfiguration.newInstance({
                                                                            ...matchingConfig,
                                                                            id          : matchingRecord?.sysId,
                                                                            pointBinding: nodeBinding
                                                                        });
            }
        }();

        for (let sourceExt of boundSourceExts)
        {
            let model         = sourceExt.model;
            let contextNodeId = sourceGenerator.getId(sourceExt.contextId, model.pointBinding);
            sourceGenerator.pushConfig(contextNodeId, model);
            sourceGenerator.ensureTemplate(model.pointBinding, model);
        }

        let keepSources = await this.getStandardGraphSources(nodeBindings, sourceGenerator, dimension);
        return this.getStandardGraphSourceModificationRequest(keepSources);
    }

    private async getStandardGraphSources(nodeBindings: Models.AssetGraphBinding[],
                                          sourceGenerator: AssetGraphLineSourceGenerator,
                                          dimension: string): Promise<TimeSeriesSourceConfigurationExtended[]>
    {
        let contextResponses = await this.getAssetGraphResponses();

        let keepSources: TimeSeriesSourceConfigurationExtended[] = [];
        await inParallel(contextResponses, async (entry) =>
        {
            let context = entry.context;
            if (!(context instanceof Models.AssetGraphContextAsset)) return;

            const resolvedGraph = this.resolvedGraphs.get(context.graphId);
            if (!resolvedGraph) return;

            const contextSysId = context.sysId;

            const matchingGraphs = entry.responses;
            await inParallel(matchingGraphs, async (matchingGraph) =>
            {
                let primaryAsset = await matchingGraph.getPrimaryAsset();
                let rootName     = primaryAsset.model.name;

                await inParallel(nodeBindings, async (nodeBinding) =>
                {
                    if (nodeBinding.graphId !== context.graphId || context.nodeId !== resolvedGraph.getRootNodeId(nodeBinding.nodeId)) return;

                    let matchingRecords = matchingGraph.resolveInputIdentities(nodeBinding);
                    if (matchingRecords.length === 0) matchingRecords = [null]; // try to create a placeholder
                    await inParallel(matchingRecords, async (matchingRecord) =>
                    {
                        let source = sourceGenerator.getNextSource(matchingRecord, nodeBinding, contextSysId);
                        if (!source) return;

                        let sourceExt: TimeSeriesSourceConfigurationExtended;
                        if (source.id)
                        {
                            let meta                = await ControlPointMetadata.fromId(this.app, source.id);
                            source.dimension        = dimension;
                            sourceExt               = await TimeSeriesSourceConfigurationExtended.newInstance(source, this, meta, contextSysId);
                            sourceExt.rootAssetName = rootName;
                        }
                        else
                        {
                            source.dimension = AssetGraphResponseExtended.placeholderDimension;
                            sourceExt        = await TimeSeriesSourceConfigurationExtended.newInstance(source, this, null, contextSysId);
                        }
                        keepSources.push(sourceExt);
                    });
                });
            });
        });

        return keepSources;
    }

    private getStandardGraphSourceModificationRequest(keepSources: TimeSeriesSourceConfigurationExtended[]): SourceModificationRequest
    {
        let removeSources = this.sourcesExt.filter((removalCandidateExt) =>
                                                   {
                                                       let removalCandidate        = removalCandidateExt.model;
                                                       let removalCandidateBinding = removalCandidate.pointBinding;
                                                       if (!removalCandidateBinding) return false;

                                                       if (keepSources.indexOf(removalCandidateExt) >= 0) return false;

                                                       return keepSources.every((keepSourceExt: TimeSeriesSourceConfigurationExtended) =>
                                                                                {
                                                                                    if (removalCandidateExt.contextId !== keepSourceExt.contextId) return true;

                                                                                    let keepSource = keepSourceExt.model;
                                                                                    if (removalCandidate.id !== keepSource.id) return true;

                                                                                    let keepBinding = keepSource.pointBinding;
                                                                                    if (!keepBinding) return true;

                                                                                    return removalCandidateBinding.graphId !== keepBinding.graphId ||
                                                                                           removalCandidateBinding.nodeId !== keepBinding.nodeId;
                                                                                });
                                                   });

        return new SourceModificationRequest(keepSources, removeSources);
    }

    public async applyStandardGraphSourceChanges(bindings: Models.AssetGraphBinding[]): Promise<SourceModificationResult>
    {
        let sourceChanges = await this.standardGraphSourceChanges(bindings, DeviceElementExtended.PRESENT_VALUE);

        // Apply the desired changes
        return await this.applySourceChanges(sourceChanges.adds, sourceChanges.removals);
    }

    //--//

    public async gpsSourceChanges(latitudeIds: string[],
                                  dimension: string): Promise<SourceModificationRequest>
    {
        let addSources = await mapInParallel(latitudeIds, async (sourceId) =>
        {
            let meta = await ControlPointMetadata.fromId(this.app, sourceId);
            return this.buildCoordinateSourceExt(meta, undefined, dimension);
        });

        let newSources    = new Set<string>(latitudeIds);
        let removeSources = this.sourcesExt.filter((sourceExt) => !newSources.has(sourceExt.model.id));
        return new SourceModificationRequest(addSources, removeSources);
    }

    public async changeGpsSources(latitudeIds: string[],
                                  dimension = DeviceElementExtended.PRESENT_VALUE): Promise<SourceModificationResult>
    {
        let sourceChanges = await this.gpsSourceChanges(latitudeIds, dimension);
        let result        = await this.applySourceChanges(sourceChanges.adds, sourceChanges.removals);
        if (result.added.length > 0 || result.removed.length > 0)
        {
            await this.setMapSources(true);
            this.configChanged.next(false);
        }

        return result;
    }

    public nextBestSourceColor(extraUsedColors?: string[]): string
    {
        const allColors = (extraUsedColors ?? []).concat(this.dataSources.map((source) => source.color));
        return ChartColorUtilities.nextBestColor(allColors, <PaletteId>this.model.palette);
    }

    public static async getAvailableGps(domain: AppDomainContext): Promise<Models.RecordIdentity[]>
    {
        let gpsId = await domain.normalization.getWellKnownEquipmentClassId(Models.WellKnownEquipmentClass.GPS);
        if (gpsId)
        {
            let tag          = new ConditionNode(ConditionNodeType.EQUIPMENT);
            tag.value        = gpsId;
            let listResponse = await domain.assets.getList(Models.AssetFilterRequest.newInstance({tagsQuery: tag.toModel()}));
            return listResponse.results;
        }

        return [];
    }
}

abstract class AssetGraphLineSourceGenerator
{
    readonly idToConfigs: Lookup<Models.TimeSeriesSourceConfiguration[]>   = {};
    readonly bindingToConfig: Lookup<Models.TimeSeriesSourceConfiguration> = {};

    constructor()
    {
    }

    pushConfig(contextNodeId: string,
               model: Models.TimeSeriesSourceConfiguration)
    {
        let lst = this.idToConfigs[contextNodeId];
        if (!lst)
        {
            lst                             = [];
            this.idToConfigs[contextNodeId] = lst;
        }
        lst.push(Models.TimeSeriesSourceConfiguration.deepClone(model));
    }

    ensureTemplate(nodeBinding: Models.AssetGraphBinding,
                   model: Models.TimeSeriesSourceConfiguration): Models.TimeSeriesSourceConfiguration
    {
        let nodeId                 = AssetGraphTreeNode.getIdFromBinding(nodeBinding);
        let matchingConfigTemplate = this.bindingToConfig[nodeId];
        if (!matchingConfigTemplate)
        {
            matchingConfigTemplate = Models.TimeSeriesSourceConfiguration.newInstance({
                                                                                          ...(model || {}),
                                                                                          uuid: UUID.UUID()
                                                                                      });

            this.bindingToConfig[nodeId] = matchingConfigTemplate;
        }

        return matchingConfigTemplate;
    }

    abstract getId(contextSysId: string,
                   nodeBinding: Models.AssetGraphBinding): string;

    abstract buildNewSource(matchingRecord: Models.RecordIdentity,
                            nodeBinding: Models.AssetGraphBinding,
                            matchingConfig: Models.TimeSeriesSourceConfiguration): Models.TimeSeriesSourceConfiguration;


    /**
     *
     * @param matchingRecord -- null when there are no matched sources: will attempt to create placeholder
     * @param nodeBinding
     * @param contextSysId
     */
    public getNextSource(matchingRecord: Models.RecordIdentity,
                         nodeBinding: Models.AssetGraphBinding,
                         contextSysId: string): Models.TimeSeriesSourceConfiguration
    {
        let matchingConfig: Models.TimeSeriesSourceConfiguration;

        let id      = this.getId(contextSysId, nodeBinding);
        let configs = this.idToConfigs[id] || [];
        if (configs.length == 0)
        {
            matchingConfig = null;
        }
        else
        {
            let pos = configs.findIndex((config) => config.id == matchingRecord?.sysId);
            if (pos >= 0)
            {
                // Matching config found, pop it from the list.
                matchingConfig = configs[pos];
                configs.splice(pos, 1);
            }
            else if (configs.length > 1)
            {
                // No matching config found, we have more than one config, pop the first from the list.
                matchingConfig = configs[0];
                configs.splice(0, 1);
            }
            else
            {
                // Only one config left, leave it in the list, but clone it to reset the color.
                matchingConfig = configs[0];
                if (matchingConfig.color)
                {
                    // have already used all the previous sources that are associated with this nodeBinding (or configs is empty)
                    configs[0] = Models.TimeSeriesSourceConfiguration.newInstance({
                                                                                      ...matchingConfig,
                                                                                      id   : null,
                                                                                      color: null
                                                                                  });
                }
            }
        }

        return this.buildNewSource(matchingRecord, nodeBinding, matchingConfig);
    }
}

export interface AssetGraphOptionList
{
    graphId: string;
    rootId: string;
    name: string;
    binding: Models.AssetGraphBinding;
    list: ControlOption<string>[];
}

export class AssetSelectionHelper
{
    selectionChanged = new Subject<void>();

    private m_initialAssetStructureContextPlurality = new Map<string, boolean>();
    initialGpsContextPlurality: boolean;

    private m_gpsLookup: Lookup<ControlOption<string>> = {};
    gpsOptions: ControlOption<string>[]                = [];
    selectedGps: string[]                              = [];

    private m_selectedGraphIds = new Map<string, string[]>();
    private m_allAssetOptions  = new Map<string, ControlOption<string>[]>();

    private m_currentAssetOptions: AssetGraphOptionList[] = [];

    public setAssetOptions(map: Map<Models.AssetGraphBinding, ControlOption<string>[]>)
    {
        this.m_allAssetOptions = new Map<string, ControlOption<string>[]>();
        for (let [binding, options] of map.entries())
        {
            this.m_allAssetOptions.set(AssetGraphTreeNode.getIdFromBinding(binding), options);
        }

        this.syncAssetNodeSelections();
    }

    public resolveAssetOptions(roots: Models.AssetGraphBinding[],
                               nodeNameCallback: (graph: string,
                                                  root: string) => string): AssetGraphOptionList[]
    {
        this.m_currentAssetOptions = [];
        for (let binding of roots)
        {
            const graphId = binding.graphId;
            const root    = binding.nodeId;
            this.m_currentAssetOptions.push({
                                                graphId: graphId,
                                                rootId : root,
                                                binding: binding,
                                                name   : nodeNameCallback(graphId, root),
                                                list   : this.getAssetOptions(binding)
                                            });
        }

        this.m_currentAssetOptions.sort((a,
                                         b) => UtilsService.compareStrings(a.name, b.name, true));

        return this.m_currentAssetOptions;
    }

    public get currentAssetOptions(): AssetGraphOptionList[]
    {
        return this.m_currentAssetOptions;
    }

    public getAssetOptions(binding: Models.AssetGraphBinding): ControlOption<string>[]
    {
        return this.m_allAssetOptions.get(AssetGraphTreeNode.getIdFromBinding(binding)) || [];
    }

    trackOptionList(index: number,
                    list: AssetGraphOptionList)
    {
        return AssetGraphTreeNode.getIdFromBinding(list.binding);
    }

    getAssetNodeSelection(binding: Models.AssetGraphBinding): string[]
    {
        return this.m_selectedGraphIds.get(AssetGraphTreeNode.getIdFromBinding(binding)) ?? [];
    }

    public setAllAssetNodeSelections(graphIds: Map<Models.AssetGraphBinding, string[]>)
    {
        this.m_selectedGraphIds = new Map<string, string[]>();
        for (let [binding, selections] of graphIds.entries())
        {
            const key = AssetGraphTreeNode.getIdFromBinding(binding);
            this.m_selectedGraphIds.set(key, selections);

            if (!this.m_initialAssetStructureContextPlurality.has(key)) this.m_initialAssetStructureContextPlurality.set(key, selections.length > 1);
        }

        this.syncAssetNodeSelections();
    }

    public getInitialAssetStructurePlurality(binding: Models.AssetGraphBinding): boolean
    {
        return this.m_initialAssetStructureContextPlurality.get(AssetGraphTreeNode.getIdFromBinding(binding));
    }

    public getAssetNodeSelections(binding: Models.AssetGraphBinding): string[]
    {
        return this.m_selectedGraphIds.get(AssetGraphTreeNode.getIdFromBinding(binding)) || [];
    }

    public setAssetNodeSelections(binding: Models.AssetGraphBinding,
                                  selection: string[])
    {
        this.m_selectedGraphIds.set(AssetGraphTreeNode.getIdFromBinding(binding), selection);

        this.selectionChanged.next();
    }

    public forEachAssetNodeSelection(callback: (graph: string,
                                                node: string,
                                                selections: string[]) => void)
    {
        for (let [key, selections] of this.m_selectedGraphIds.entries())
        {
            const binding = AssetGraphTreeNode.getBinding(key);
            callback(binding.graphId, binding.nodeId, selections);
        }
    }

    private syncAssetNodeSelections()
    {
        let previousRoots = new Set<string>(this.m_selectedGraphIds.keys() ?? []);
        for (let [key, availableOptions] of this.m_allAssetOptions.entries())
        {
            previousRoots.delete(key);

            if (availableOptions.length === 0)
            {
                this.m_selectedGraphIds.delete(key);
            }
            else
            {
                let selectedGraphIds = this.m_selectedGraphIds.get(key);

                selectedGraphIds = selectedGraphIds?.filter((id) => availableOptions.find((opt) => opt.id === id));

                if (!selectedGraphIds?.length)
                {
                    selectedGraphIds = [availableOptions[0].id];
                }

                this.m_selectedGraphIds.set(key, selectedGraphIds);
            }
        }

        for (let key of previousRoots)
        {
            this.m_selectedGraphIds.delete(key);
        }
    }

    //--//

    public async ensureGpsOptions(app: AppContext,
                                  gpsAssets: AssetExtended[])
    {
        if (!this.gpsOptions.length && gpsAssets?.length)
        {
            this.m_gpsLookup = {};
            this.gpsOptions  = [];

            let locationIdentities = gpsAssets.map((gps) => gps.model.location);
            let locationExts       = await app.domain.assets.getTypedExtendedBatch(LocationExtended, locationIdentities);
            await inParallel(locationExts, async (locationExt,
                                                  idx) =>
            {
                let gpsId  = gpsAssets[idx].model.sysId;
                let option = new ControlOption(gpsId, await locationExt.getRecursiveName());

                this.gpsOptions.push(option);
                this.m_gpsLookup[gpsId] = option;
            });
        }
    }

    public async ensureGpsSelection(currSelectedGpsFn: () => Promise<string[]>): Promise<boolean>
    {
        if (!this.selectedGps.length)
        {
            let results = await currSelectedGpsFn();
            results     = results.map((result) => this.m_gpsLookup[result]?.id);
            if (!results.length && this.gpsOptions.length) results = [this.gpsOptions[0].id];

            this.selectedGps = results;
        }

        if (this.initialGpsContextPlurality === undefined) this.initialGpsContextPlurality = this.selectedGps.length > 1;

        return true;
    }

    public forEachGpsSelection(callback: (gpsId: string,
                                          option: ControlOption<string>) => void)
    {
        for (let gps of this.selectedGps) callback(gps, this.m_gpsLookup[gps]);
    }

    public setSelectedGps(selection: string[])
    {
        this.selectedGps = selection || [];

        this.selectionChanged.next();
    }
}

export interface InteractableSource
{
    identifier: string;

    name: string;
    description: string;

    panel: number;
    color: string;
    colorStops?: ColorGradientStop[];

    valid: boolean;

    deviceElementId?: string;
    timeOffset?: Models.TimeDuration;

    getChartData(): ChartPointSource<any>;
}

export interface InteractableSourcesChart
{
    sourceStatesUpdated: EventEmitter<Lookup<VisualizationDataSourceState>>;

    chartUpdated: EventEmitter<boolean>;

    isReady(): boolean;

    onChange(): void;

    getNumSources(): number;

    getSourceState(sourceId: string): VisualizationDataSourceState;

    configureSource(sourceId: string): void;

    isDeletable(sourceId: string): boolean;

    getSource(sourceId: string): InteractableSource;

    getSources(panelIdx: number,
               onlyVisible: boolean): InteractableSource[];

    toggleTarget(sourceId: string,
                 fromMouseover: boolean): void;

    toggleEnabled(sourceId: string): void;

    multiToggleEnabled(originSourceId: string): void;
}

export class DeletionAffectedAnnotation
{
    constructor(public readonly idx: number,
                public readonly selection: CanvasZoneSelection,
                public readonly annotation: Models.TimeSeriesAnnotationConfiguration,
                public readonly displayFactors?: Models.EngineeringUnitsFactors)
    {
    }
}

export class TimeSeriesSourceConfigurationExtended implements InteractableSource
{
    public ownerPanel: TimeSeriesPanelConfigurationExtended;
    public mapSource: AssetExtended;
    public includeAlerts: boolean = true;

    private m_cachedData: ChartPointSource<any> = new PlaceHolderSource<any>(null, null);
    private m_cachedDataParameters: TimeSeriesSourceParameters;

    public dataSourceReady = new Future<void>();
    private m_dataSource: TimeSeriesSource;
    public get dataSource(): TimeSeriesSource
    {
        return this.m_dataSource;
    }

    public set dataSource(value: TimeSeriesSource)
    {
        this.m_dataSource = value;
        if (value)
        {
            this.dataSourceReady.resolve();
        }
    }

    get hasMinimumData(): boolean
    {
        if (!this.model.id) return this.model.dimension === AssetGraphResponseExtended.placeholderDimension;
        return !!this.model.dimension;
    }

    public readonly onDelete                                 = new Future<void>();
    private readonly m_deleter                               = new Debouncer(7500, () => this.completeDeletion());
    public affectedAnnotations: DeletionAffectedAnnotation[] = [];

    get deleted(): boolean
    {
        return this.onDelete.isResolved();
    }

    get markedForDeletion(): boolean
    {
        return this.m_deleter.scheduled;
    }

    private m_associatedGroup: Models.TimeSeriesAxisGroupConfiguration;

    get unitsFactors(): Models.EngineeringUnitsFactors
    {
        return this.m_associatedGroup?.selectedFactors || this.m_family;
    }

    private m_panelSourceIndex: number;
    get panelSourceIndex(): number
    {
        return this.m_panelSourceIndex;
    }

    set panelSourceIndex(index: number)
    {
        this.m_panelSourceIndex = index;
        this.m_cachedData.index = index;
    }

    public rootAssetName: string;

    get name(): string
    {
        if (this.model.pointBinding && this.rootAssetName) return `${this.rootAssetName} - ${this.m_sourceName}`;
        return this.m_sourceName;
    }

    private constructor(public model: Models.TimeSeriesSourceConfiguration,
                        private readonly m_sourceName: string,
                        public readonly description: string,
                        public readonly contextId: string,
                        private readonly m_family: Models.EngineeringUnitsFactors)
    {
        // backwards compatibility
        if (!this.model.uuid) this.model.uuid = UUID.UUID();

        if (!this.m_sourceName) this.m_sourceName = "";
        if (!this.description) this.description = "";
    }

    public static computeIdentifier(source: Models.TimeSeriesSourceConfiguration): string
    {
        return source.uuid + "/" + source.id;
    }

    public static newModel(model?: Models.TimeSeriesSourceConfiguration): Models.TimeSeriesSourceConfiguration
    {
        return Models.TimeSeriesSourceConfiguration.newInstance({
                                                                    uuid             : model?.uuid || UUID.UUID(),
                                                                    id               : model?.id,
                                                                    dimension        : model?.dimension || DeviceElementExtended.PRESENT_VALUE,
                                                                    color            : null,
                                                                    showDecimation   : false,
                                                                    decimationDisplay: Models.TimeSeriesDecimationDisplay.Average,
                                                                    timeOffset       : TimeDurationExtended.newModel()
                                                                });
    }

    public static async newInstance(model: Models.TimeSeriesSourceConfiguration,
                                    owner: TimeSeriesChartConfigurationExtended,
                                    meta: ControlPointMetadata,
                                    contextId: string): Promise<TimeSeriesSourceConfigurationExtended>
    {
        model ||= TimeSeriesSourceConfigurationExtended.newModel();

        let sourceName: string;
        let sourceDescription: string;
        let schema: Models.TimeSeriesPropertyType;
        if (meta)
        {
            if (owner?.model.type === Models.TimeSeriesChartType.COORDINATE)
            {
                sourceName             = meta.locationName;
                sourceDescription      = meta.fullLocationName;
                const separator        = " - ";
                let localLocationIdx   = sourceDescription.indexOf(separator + sourceName);
                let restOfFullLocation = sourceDescription.substring(0, localLocationIdx);
                if (sourceName.length + restOfFullLocation.length === sourceDescription.length - separator.length) sourceDescription = restOfFullLocation;
            }
            else
            {
                sourceName        = meta.name;
                sourceDescription = meta.standardDescription();
                schema            = await meta.point.getSchemaProperty(DeviceElementExtended.PRESENT_VALUE);
            }
        }
        else if (model.pointBinding && model.dimension === AssetGraphResponseExtended.placeholderDimension)
        {
            let pointBinding = model.pointBinding;
            let graph        = owner?.resolvedGraphs.get(pointBinding.graphId);
            if (!graph && owner)
            {
                await owner.loadGraphs();
                graph = owner.resolvedGraphs.get(pointBinding.graphId);
            }
            sourceName = graph ? `${graph.getNodeName(pointBinding.nodeId)} (Missing)` : "Placeholder Source (Missing in this context)";
        }

        return new TimeSeriesSourceConfigurationExtended(model, sourceName, sourceDescription, contextId, schema?.unitsFactors);
    }

    public static async resolveFromIdAndDimension(host: TimeSeriesSourceHost,
                                                  id: string,
                                                  dimension?: string): Promise<TimeSeriesSourceConfigurationExtended>
    {
        if (id)
        {
            let info = await TimeSeriesSource.sourceFromId(host, id);
            if (info)
            {
                return await info.generateExtended(dimension);
            }
        }

        return null;
    }

    attemptAdoptCachedData(other: TimeSeriesSourceConfigurationExtended)
    {
        if (this.identifier != other?.identifier) return;

        this.updateAssociatedGroup();

        if (this.copySourceData(other))
        {
            this.m_cachedDataParameters = {...other.m_cachedDataParameters};
        }
    }

    copySourceData(other: TimeSeriesSourceConfigurationExtended): boolean
    {
        if (this.model.id === other.model.id)
        {
            this.m_cachedData = other.m_cachedData;
            this.dataSource   = other.dataSource;
            return true;
        }

        return false;
    }

    async loadMapSource(deviceElemExt: DeviceElementExtended): Promise<AssetExtended>
    {
        if (deviceElemExt instanceof DeviceElementExtended && deviceElemExt.model.sysId === this.model.id && deviceElemExt.isCoordinate)
        {
            let parentHolder = await deviceElemExt.getExtendedParentsOfRelation(Models.AssetRelationship.controls);
            if (parentHolder) this.mapSource = parentHolder[0];
        }

        return this.mapSource;
    }

    async bindToDataSource(host: TimeSeriesSourceHost): Promise<TimeSeriesSource>
    {
        if (!this.dataSource)
        {
            try
            {
                this.dataSource = await TimeSeriesSource.sourceFromId(host, this.model.id);
            }
            catch (e)
            {
                // Ignore failures.
            }
        }

        return this.dataSource;
    }

    updateAssociatedGroup()
    {
        let owner = this.ownerPanel?.owner;
        if (!owner) return;

        let panelExt           = owner.panelsExt[this.model.panel];
        let axisExt            = this.model.axis === 0 ? panelExt.leftAxisExtended : panelExt.rightAxisExtended;
        this.m_associatedGroup = axisExt.model.groupedFactors[axisExt.indexOfGroup(this.m_family)];
    }

    async markAsDeleted(app: AppContext,
                        chart: TimeSeriesChartComponent,
                        allAffectedAnnotations: boolean): Promise<VisualizationDataSourceState>
    {
        if (this.markedForDeletion) return VisualizationDataSourceState.Deleted;

        this.m_deleter.invoke();

        let axis        = this.ownerPanel.axisByIndexExtended(this.model.axis);
        let axisChanged = await axis.sourceMarkedForDeletion(app.domain.units, this);
        let state       = chart.optio3Chart.markSourceDeleted(this.getChartData(), this.model.panel, !axisChanged);

        let sources = this.ownerPanel.owner.dataSources;
        sources.splice(sources.indexOf(this.model), 1);

        for (let i = this.affectedAnnotations.length - 1; i >= 0; i--)
        {
            let affectedAnnotation = this.affectedAnnotations[i];
            if (allAffectedAnnotations || !affectedAnnotation.displayFactors)
            {
                chart.deleteAnnotation(affectedAnnotation.selection, affectedAnnotation.idx);
            }
        }

        this.m_cachedData.enabled = false;

        chart.sourcesChanged();
        if (axisChanged) chart.onChange();
        chart.chartUpdated.emit();

        return state;
    }

    async completeDeletion()
    {
        if (!this.deleted)
        {
            this.onDelete.resolve();

            await this.ownerPanel.owner.applySourceChanges([], [this]);
        }
    }

    async cancelDeletion(units: UnitsService,
                         chart: TimeSeriesChartComponent): Promise<VisualizationDataSourceState>
    {
        // Only cancel if deleting
        if (this.markedForDeletion)
        {
            // Clear the deletion timer
            if (this.clearDeleter())
            {
                // Re-enable cached data
                this.m_cachedData.enabled = true;

                let chartExt                        = this.ownerPanel.owner;
                let identifierToIdx: Lookup<number> = {};
                for (let i = 0; i < chartExt.sourcesExt.length; i++) identifierToIdx[chartExt.sourcesExt[i].identifier] = i;
                chartExt.dataSources.push(this.model);

                chartExt.dataSources.sort((aSource,
                                           bSource) =>
                                          {
                                              let aId = TimeSeriesSourceConfigurationExtended.computeIdentifier(aSource);
                                              let bId = TimeSeriesSourceConfigurationExtended.computeIdentifier(bSource);
                                              return UtilsService.compareNumbers(identifierToIdx[aId], identifierToIdx[bId], true);
                                          });

                let state = chart.optio3Chart.cancelDeletion(this.getChartData(), this.model.panel);

                let axisUpdated          = false;
                let someAnnotationsAdded = false;
                for (let affected of this.affectedAnnotations)
                {
                    let addBack = !affected.displayFactors;
                    if (!addBack && chart.annotations.indexOf(affected.selection) < 0)
                    {
                        let leftAxisExt         = this.ownerPanel.leftAxisExtended;
                        let equivalentAxisUnits = UnitsService.areEquivalent(leftAxisExt.model.displayFactors, affected.displayFactors);
                        if (equivalentAxisUnits || leftAxisExt.lastSourceDeleting)
                        {
                            leftAxisExt.lastSourceDeleting = false;

                            if (!equivalentAxisUnits)
                            {
                                leftAxisExt.model.displayFactors = affected.displayFactors;
                                axisUpdated                      = true;
                            }

                            addBack = true;
                        }
                    }

                    if (addBack)
                    {
                        let index = chart.reinsertAnnotation(affected, this.model.panel);
                        chartExt.model.annotations.splice(index, 0, affected.annotation);

                        someAnnotationsAdded = true;
                    }
                }

                if (!axisUpdated)
                {
                    let axis = this.ownerPanel.axisByIndexExtended(this.model.axis);
                    if (axis.lastSourceDeleting)
                    {
                        axis.model.displayFactors = this.unitsFactors;
                        axis.lastSourceDeleting   = false;
                        axisUpdated               = true;
                    }
                    else
                    {
                        axisUpdated = UnitsService.areEquivalent(axis.model.displayFactors, this.unitsFactors);
                    }
                }

                if (axisUpdated || someAnnotationsAdded) chart.onChange();

                chart.sourcesChanged();
                chart.chartUpdated.emit();
                return state;
            }
        }

        return null;
    }

    clearDeleter(): boolean
    {
        if (!this.deleted)
        {
            this.m_deleter.cancel();
            return true;
        }
        return false;
    }

    async fetch(host: TimeSeriesSourceHost,
                range: RangeSelectionExtended,
                clearCache?: boolean)
    {
        let dataSource = await this.bindToDataSource(host);
        if (!dataSource) return;

        let dimension    = this.model.dimension;
        let unitsFactors = this.unitsFactors;

        let min = range.getMin();
        let max = range.getMax();

        //
        // If the time range includes 'now', leave the max undefined, to allow new samples to flow in.
        //
        let timeRange = range.rangeResolved;
        if (timeRange?.id != TimeRangeId.CustomRange && !timeRange.usePreviousRange)
        {
            max = undefined;
        }

        if (this.model.showMovingAverage > 0)
        {
            min = MomentHelper.add(min, -this.model.showMovingAverage, "seconds");
        }

        let parameters: TimeSeriesSourceParameters = {
            property: dimension,
            units   : unitsFactors,

            rangeStart: min,
            rangeEnd  : max,
            timeOffset: this.model.timeOffset,

            includeAlerts: this.includeAlerts,

            showMovingAverage    : this.model.showMovingAverage,
            onlyShowMovingAverage: this.model.onlyShowMovingAverage
        };
        if (clearCache) this.m_cachedDataParameters = null;

        if (this.shouldRefreshCachedData(parameters))
        {
            let enabled = this.m_cachedData.enabled;

            let cachedData = await this.dataSource.getDataSource(parameters);
            if (cachedData)
            {
                cachedData.index                 = this.panelSourceIndex;
                cachedData.enabled               = enabled;
                cachedData.showMovingAverage     = parameters.showMovingAverage;
                cachedData.onlyShowMovingAverage = parameters.onlyShowMovingAverage;
            }
            else
            {
                cachedData = new PlaceHolderSource<any>(null, null);
                parameters = null;
            }

            this.m_cachedData           = cachedData;
            this.m_cachedDataParameters = parameters;
        }
    }

    private shouldRefreshCachedData(parameters: TimeSeriesSourceParameters): boolean
    {
        if (!this.m_cachedData.isValid() || !this.m_cachedDataParameters) return true;

        if (this.dataSource?.meta.point instanceof MetricsDeviceElementExtended)
        {
            // Metrics change values based on the start of the range. Strict matching.
            return !TimeSeriesSourceParameters.isCompatible(this.m_cachedDataParameters, parameters, false);
        }
        else
        {
            // Fuzzy matching, allowing start to slip forward.
            return !TimeSeriesSourceParameters.isCompatible(this.m_cachedDataParameters, parameters, true);
        }
    }

    getChartData(): ChartPointSource<any>
    {
        return this.m_cachedData;
    }

    updateDataConfig(display: Models.TimeSeriesDisplayConfiguration,
                     zone?: string)
    {
        let data = this.m_cachedData;
        if (data && display)
        {
            data.fillArea        = display.fillArea;
            data.hideDecimation  = display.hideDecimation;
            data.autoAggregation = display.automaticAggregation;

            if (zone) data.zoneName = zone;
            if (this.model.showDecimation) data.hideDecimation = false;

            let range          = this.model.range;
            data.rangeOverride = ToggleableNumericRangeExtended.isActive(range) ? new ToggleableNumericRangeExtended(range).chartRange : null;
            data.color         = this.model.color;

            data.decimationDisplay     = this.model.decimationDisplay;
            data.showMovingAverage     = this.model.showMovingAverage;
            data.onlyShowMovingAverage = this.model.onlyShowMovingAverage;

            data.zoomable = false;
        }
    }

    updateZoomable(zoomable: boolean)
    {
        if (this.m_cachedData)
        {
            this.m_cachedData.zoomable = zoomable;
        }
    }

    updateDeletedState()
    {
        if (this.markedForDeletion) this.getChartData().state = VisualizationDataSourceState.Deleted;
    }

    //--//

    get identifier(): string { return TimeSeriesSourceConfigurationExtended.computeIdentifier(this.model); }

    get deviceElementId(): string { return this.model.id; }

    get panel(): number { return this.model.panel || 0; }

    get valid(): boolean { return this.model.dimension != AssetGraphResponseExtended.placeholderDimension; }

    get color(): string { return this.model.color; }

    get timeOffset(): Models.TimeDuration { return this.model.timeOffset; }
}

export class TimeSeriesDisplayConfigurationExtended
{
    constructor(public model: Models.TimeSeriesDisplayConfiguration)
    {
    }

    static newModel(): Models.TimeSeriesDisplayConfiguration
    {
        return Models.TimeSeriesDisplayConfiguration.newInstance({
                                                                     title               : null,
                                                                     size                : 425,
                                                                     fillArea            : false,
                                                                     hideDecimation      : false,
                                                                     automaticAggregation: false,
                                                                     showAlerts          : true
                                                                 });
    }

    static newInstance(model?: Models.TimeSeriesDisplayConfiguration): TimeSeriesDisplayConfigurationExtended
    {
        return new TimeSeriesDisplayConfigurationExtended(model || this.newModel());
    }
}

export class TimeSeriesPanelConfigurationExtended
{
    public index: number;
    public readonly leftAxisExtended: TimeSeriesAxisConfigurationExtended;
    public readonly rightAxisExtended: TimeSeriesAxisConfigurationExtended;

    public sources: TimeSeriesSourceConfigurationExtended[]         = [];
    public annotations: TimeSeriesAnnotationConfigurationExtended[] = [];

    constructor(public readonly owner: TimeSeriesChartConfigurationExtended,
                public readonly model: Models.TimeSeriesPanelConfiguration)
    {
        this.leftAxisExtended  = TimeSeriesAxisConfigurationExtended.newInstance(this, model.leftAxis);
        this.rightAxisExtended = TimeSeriesAxisConfigurationExtended.newInstance(this, model.rightAxis);
    }

    public static newModel(): Models.TimeSeriesPanelConfiguration
    {
        return Models.TimeSeriesPanelConfiguration.newInstance({
                                                                   leftAxis : TimeSeriesAxisConfigurationExtended.newModel(),
                                                                   rightAxis: TimeSeriesAxisConfigurationExtended.newModel()
                                                               });
    }

    public static newInstance(owner: TimeSeriesChartConfigurationExtended,
                              model?: Models.TimeSeriesPanelConfiguration): TimeSeriesPanelConfigurationExtended
    {
        return new TimeSeriesPanelConfigurationExtended(owner, model || this.newModel());
    }

    public static areEquivalent(units: UnitsService,
                                panelA: Models.TimeSeriesPanelConfiguration,
                                panelB: Models.TimeSeriesPanelConfiguration,
                                considerGroupedFactors: boolean,
                                considerX: boolean): boolean
    {
        if (!TimeSeriesAxisConfigurationExtended.areEquivalent(units, panelA.leftAxis, panelB.leftAxis, considerGroupedFactors)) return false;
        if (!TimeSeriesAxisConfigurationExtended.areEquivalent(units, panelA.rightAxis, panelB.rightAxis, considerGroupedFactors)) return false;

        return !considerX || TimeSeriesAxisConfigurationExtended.areEquivalent(units, panelA.xAxis, panelB.xAxis, considerGroupedFactors);
    }

    public setIndex(index: number)
    {
        this.index = index;
        for (let source of this.sources) source.model.panel = index;
        for (let annotation of this.annotations) annotation.model.panel = index;
    }

    public addSource(source: TimeSeriesSourceConfigurationExtended)
    {
        source.ownerPanel = this;
        this.sources.push(source);
        source.panelSourceIndex = this.sources.length - 1;
    }

    public removeSource(source: TimeSeriesSourceConfigurationExtended)
    {
        let pos = this.sources.findIndex((ext) => ext.model === source.model);
        if (pos >= 0)
        {
            let sourceExt        = this.sources[pos];
            sourceExt.ownerPanel = null;

            this.sources.splice(pos, 1);
            for (; pos < this.sources.length; pos++) this.sources[pos].panelSourceIndex = pos;
        }
    }

    public async filterSources(units: UnitsService,
                               axis: TimeSeriesAxisConfigurationExtended,
                               family: Models.EngineeringUnitsFactors,
                               ...excludes: TimeSeriesSourceConfigurationExtended[]): Promise<TimeSeriesSourceConfigurationExtended[]>
    {
        let axisIndex = this.indexOfAxis(axis);
        let familyExt = await units.resolveRootDescriptor(family);

        return filterAsync(this.sources, async (ext: TimeSeriesSourceConfigurationExtended) =>
        {
            if (axis && ext.model.axis !== axisIndex) return false;

            if (family)
            {
                let unitsExt = await units.resolveRootDescriptor(ext.unitsFactors);
                if (unitsExt != familyExt) return false;
            }

            return excludes.every((exclude) => exclude !== ext);
        }) || [];
    }

    //--//

    indexOfAxis(axis: TimeSeriesAxisConfigurationExtended): number
    {
        if (axis)
        {
            if (this.leftAxisExtended.model === axis.model) return 0;
            if (this.rightAxisExtended.model === axis.model) return 1;
        }
        return -1;
    }

    axisByIndexExtended(index: number): TimeSeriesAxisConfigurationExtended
    {
        if (index === 0) return this.leftAxisExtended;
        if (index === 1) return this.rightAxisExtended;
        return null;
    }

    async addNewSource(app: AppContext,
                       chart: TimeSeriesChartConfigurationExtended,
                       sourceExt: TimeSeriesSourceConfigurationExtended)
    {
        const source            = sourceExt.model;
        const family            = sourceExt.unitsFactors;
        const familyRootFactors = await app.domain.units.resolveRootFactors(family);

        if (isNaN(source.axis))
        {
            const leftRootFactors = await app.domain.units.resolveRootFactors(this.leftAxisExtended.model.displayFactors);
            if (UnitsService.areEquivalent(familyRootFactors, leftRootFactors))
            {
                source.axis = 0;
            }
            else if (this.rightAxisExtended.model.displayFactors)
            {
                const rightRootFactors = await app.domain.units.resolveRootFactors(this.rightAxisExtended.model.displayFactors);
                if (UnitsService.areEquivalent(familyRootFactors, rightRootFactors))
                {
                    source.axis = 1;
                }
            }

            if (isNaN(source.axis))
            {
                const numLeft  = this.sources.filter((sourceExt) => sourceExt.model.axis === 0).length;
                const numRight = this.sources.length - numLeft;
                source.axis    = numLeft <= numRight ? 0 : 1;
            }
        }

        const axisExt = source.axis === 0 ? this.leftAxisExtended : this.rightAxisExtended;
        await axisExt.sourceAdded(app, family);
        sourceExt.updateAssociatedGroup();
    }

    async cleanOldSourceAxis(units: UnitsService,
                             source: TimeSeriesSourceConfigurationExtended)
    {
        let axis = this.axisByIndexExtended(source.model.axis);
        if (axis)
        {
            // Find the sources in the axis
            let remainingSources = await this.filterSources(units, axis, undefined, source);

            // If no sources remain, re-init the axis, otherwise clean up families
            if (remainingSources.length === 0)
            {
                TimeSeriesAxisConfigurationExtended.init(axis.model);
            }
            else
            {
                let unitsToSources = await axis.getOrganizedSources(units, remainingSources, true);
                let display        = axis.model.displayFactors;
                for (let [unitsDescriptorExt, sources] of unitsToSources.entries())
                {
                    if (sources.length === 0)
                    {
                        let family = EngineeringUnitsDescriptorExtended.extractFactors(unitsDescriptorExt);
                        axis.removeGroup(family);

                        if (UnitsService.areEquivalent(family, display)) axis.model.displayFactors = null;
                    }
                }

                if (!axis.model.displayFactors && axis.model.groupedFactors?.length > 0)
                {
                    let newUnits              = await units.resolveRootDescriptor(axis.model.groupedFactors[0].keyFactors);
                    axis.model.displayFactors = EngineeringUnitsDescriptorExtended.extractFactors(newUnits);
                }
            }
        }
    }
}

export class TimeSeriesAnnotationConfigurationExtended
{
    get valid(): boolean
    {
        return this.panelExt.index >= 0;
    }

    constructor(public readonly model: Models.TimeSeriesAnnotationConfiguration,
                public readonly panelExt: TimeSeriesPanelConfigurationExtended)
    {
        this.model.panel = panelExt.index;
        panelExt.annotations.push(this);
    }
}

export class TimeSeriesAxisConfigurationExtended
{
    public lastSourceDeleting: boolean;
    public hideLabel: boolean;

    constructor(public readonly panel: TimeSeriesPanelConfigurationExtended,
                public model: Models.TimeSeriesAxisConfiguration)
    {
        if (!model.groupedFactors)
        {
            model.groupedFactors = [];
        }
    }

    public static init(model: Models.TimeSeriesAxisConfiguration)
    {
        model.displayFactors = null;
        model.groupedFactors = [];
    }

    public static newModel(): Models.TimeSeriesAxisConfiguration
    {
        let model = new Models.TimeSeriesAxisConfiguration();
        TimeSeriesAxisConfigurationExtended.init(model);
        return model;
    }

    public static newInstance(panel: TimeSeriesPanelConfigurationExtended,
                              model?: Models.TimeSeriesAxisConfiguration): TimeSeriesAxisConfigurationExtended
    {
        return new TimeSeriesAxisConfigurationExtended(panel, model || this.newModel());
    }

    public static areEquivalent(units: UnitsService,
                                axisA: Models.TimeSeriesAxisConfiguration,
                                axisB: Models.TimeSeriesAxisConfiguration,
                                considerGroupedFactors: boolean = true): boolean
    {
        if (!axisA) return !axisB;
        if (!axisB) return !axisA;

        if (!UtilsService.equivalentStrings(axisA.label, axisB.label)) return false;

        if (!ToggleableNumericRangeExtended.areEquivalent(axisA.override, axisB.override)) return false;

        if (!UnitsService.areIdentical(axisA.displayFactors, axisB.displayFactors)) return false;

        if (considerGroupedFactors)
        {
            if (axisA.groupedFactors.length !== axisB.groupedFactors.length) return false;

            for (let i = 0; i < axisA.groupedFactors.length; i++)
            {
                let groupedFactorsA = axisA.groupedFactors[i];
                let groupedFactorsB = axisB.groupedFactors[i];

                if (!UnitsService.areEquivalent(groupedFactorsA.keyFactors, groupedFactorsB.keyFactors)) return false;
                if (!UnitsService.areIdentical(groupedFactorsA.selectedFactors, groupedFactorsB.selectedFactors)) return false;
            }
        }

        return true;
    }

    async sourceAdded(app: AppContext,
                      sourceFamily: Models.EngineeringUnitsFactors)
    {
        if (!this.model.displayFactors && sourceFamily) this.model.displayFactors = sourceFamily;

        let sameAxisSources   = await this.panel.filterSources(app.domain.units, this, undefined);
        let familyRoot        = await app.domain.units.resolveRootDescriptor(sourceFamily);
        let familyRootFactors = EngineeringUnitsDescriptorExtended.extractFactors(familyRoot);
        if (this.indexOfGroup(familyRootFactors) < 0)
        {
            if (sameAxisSources.length === 1 && UnitsService.areEquivalent(sameAxisSources[0].unitsFactors, sourceFamily))
            {
                if (familyRoot?.noDimensions)
                {
                    let dimensionlessLabel = await app.domain.units.getDimensionlessFlavor(sourceFamily);
                    if (!this.model.label && dimensionlessLabel != UnitsService.noUnitsDisplayName) this.model.label = dimensionlessLabel;
                }
            }

            this.model.groupedFactors.push(Models.TimeSeriesAxisGroupConfiguration.newInstance({
                                                                                                   keyFactors     : familyRootFactors,
                                                                                                   selectedFactors: sourceFamily
                                                                                               }));
        }
    }

    async sourceMarkedForDeletion(units: UnitsService,
                                  source: TimeSeriesSourceConfigurationExtended): Promise<boolean>
    {
        let unitsFactors = source.unitsFactors;
        if (!UnitsService.areEquivalent(this.model.displayFactors, unitsFactors)) return false;

        let sameUnitsAxisSources = await source.ownerPanel.filterSources(units, this, unitsFactors, source);
        sameUnitsAxisSources     = sameUnitsAxisSources.filter((source) => !source.markedForDeletion);
        if (sameUnitsAxisSources.length === 0)
        {
            let axisSources = await source.ownerPanel.filterSources(units, this, undefined);
            axisSources     = axisSources.filter((source) => !source.markedForDeletion);

            if (axisSources.length > 0)
            {
                this.model.displayFactors = axisSources[0].unitsFactors;
            }
            else
            {
                this.lastSourceDeleting = true;
            }

            return true;
        }

        return UnitsService.areEquivalent(this.model.displayFactors, unitsFactors, true);
    }

    async getOrganizedSources(units: UnitsService,
                              sources: TimeSeriesSourceConfigurationExtended[],
                              includeSourcesMarkedForDeletion: boolean): Promise<Map<EngineeringUnitsDescriptorExtended, TimeSeriesSourceConfigurationExtended[]>>
    {
        let map = new Map<EngineeringUnitsDescriptorExtended, TimeSeriesSourceConfigurationExtended[]>();
        for (let group of this.model.groupedFactors || [])
        {
            let groupUnitsDescriptor = await units.resolveRootDescriptor(group.keyFactors);
            let equivalentSources    = await this.getSameUnitSources(units, group.keyFactors, sources);

            if (!includeSourcesMarkedForDeletion) equivalentSources = equivalentSources.filter((source) => !source.markedForDeletion);
            map.set(groupUnitsDescriptor, equivalentSources);
        }

        return map;
    }

    async getSameUnitSources(units: UnitsService,
                             unitsFactors: Models.EngineeringUnitsFactors,
                             sources: TimeSeriesSourceConfigurationExtended[]): Promise<TimeSeriesSourceConfigurationExtended[]>
    {
        let groupUnitsDescriptor = await units.resolveRootDescriptor(unitsFactors);
        return filterAsync(sources, async (source) => groupUnitsDescriptor === await units.resolveRootDescriptor(source.unitsFactors));
    }

    indexOfGroup(factors: Models.EngineeringUnitsFactors): number
    {
        return this.model.groupedFactors.findIndex((cfg) => UnitsService.areEquivalent(cfg.keyFactors, factors));
    }

    removeGroup(factors: Models.EngineeringUnitsFactors)
    {
        let index = this.indexOfGroup(factors);
        if (index >= 0) this.model.groupedFactors.splice(index, 1);
    }
}

export class ToggleableNumericRangeExtended
{
    get isActive(): boolean
    {
        return ToggleableNumericRangeExtended.isActive(this.model);
    }

    get chartRange(): ChartValueRange
    {
        let range = new ChartValueRange();
        if (!this.model.minInvalid) range.min = this.model.min;
        if (!this.model.maxInvalid) range.max = this.model.max;
        return range;
    }

    constructor(public model: Models.ToggleableNumericRange)
    {
    }

    public static areEquivalent(overrideA: Models.ToggleableNumericRange,
                                overrideB: Models.ToggleableNumericRange): boolean
    {
        let aIsActive = ToggleableNumericRangeExtended.isActive(overrideA);
        let bIsActive = ToggleableNumericRangeExtended.isActive(overrideB);
        if (aIsActive || bIsActive)
        {
            if (aIsActive && bIsActive)
            {
                if (!overrideA.minInvalid !== !overrideB.minInvalid) return false;
                if (!overrideA.maxInvalid !== !overrideB.maxInvalid) return false;

                if (overrideA.minInvalid) return overrideA.max === overrideB.max;
                if (overrideA.maxInvalid) return overrideA.min === overrideB.min;

                return overrideA.max === overrideB.max && overrideA.min === overrideB.min;
            }

            return false;
        }

        return true;
    }

    public static cleanModel(override: Models.ToggleableNumericRange): void
    {
        if (!override) return;

        override.active = ToggleableNumericRangeExtended.isActive(override);

        if (!override.active) override.min = override.max = null;
        override.minInvalid = override.min == null;
        override.maxInvalid = override.max == null;
        if (override.minInvalid && override.maxInvalid)
        {
            override.active = false;
            override.min    = override.max = null;
        }
    }

    public static isActive(override: Models.ToggleableNumericRange): boolean
    {
        if (!override) return false;
        if (!override.active) return false;

        if (!override.minInvalid && !override.maxInvalid) return override.min < override.max;

        if (!override.minInvalid) return !isNaN(override.min ?? undefined);
        if (!override.maxInvalid) return !isNaN(override.max ?? undefined);

        return false;
    }

    public static toToggleable(range: Models.NumericRange,
                               inactive?: boolean): Models.ToggleableNumericRange
    {
        if (!range) return null;
        return Models.ToggleableNumericRange.newInstance({
                                                             min       : range.min,
                                                             max       : range.max,
                                                             minInvalid: range.minInvalid,
                                                             maxInvalid: range.maxInvalid,
                                                             active    : !inactive
                                                         });
    }
}

class SourceModificationRequest
{
    constructor(public readonly adds: TimeSeriesSourceConfigurationExtended[]     = [],
                public readonly removals: TimeSeriesSourceConfigurationExtended[] = [])
    {}
}

export class SourceModificationResult
{
    added: TimeSeriesSourceConfigurationExtended[]      = [];
    removed: TimeSeriesSourceConfigurationExtended[]    = [];
    duplicates: TimeSeriesSourceConfigurationExtended[] = [];
    ignored: TimeSeriesSourceConfigurationExtended[]    = [];
}

export class TimeSeriesSourceTuple
{
    public units: Models.EngineeringUnitsFactors[];

    constructor(public host: TimeSeriesSourceHost,
                private unitsSvc: UnitsService,
                public sourceX: TimeSeriesSource,
                public sourceY: TimeSeriesSource,
                public sourceZ?: TimeSeriesSource,
                public name?: string)
    {}

    static async fromTuple(host: TimeSeriesSourceHost,
                           idTuple: ScatterPlotPropertyTuple<string>,
                           idToSeriesSource: Lookup<TimeSeriesSource>,
                           name?: string): Promise<TimeSeriesSourceTuple>
    {
        let sourceArr: TimeSeriesSource[] = [];
        let someFailed                    = false;
        if (idToSeriesSource)
        {
            let addSource = (source: TimeSeriesSource) =>
            {
                if (source)
                {
                    sourceArr.push(source);
                }
                else
                {
                    someFailed = true;
                }
            };
            addSource(idToSeriesSource[idTuple.valueX]);
            addSource(idToSeriesSource[idTuple.valueY]);
            let zId = idTuple.valueZ;
            if (zId) addSource(idToSeriesSource[zId]);
        }

        if (!idToSeriesSource || someFailed)
        {
            let uniquifier = new Set<string>();
            uniquifier.add(idTuple.valueX);
            uniquifier.add(idTuple.valueY);
            let zId = idTuple.valueZ;
            if (zId) uniquifier.add(zId);
            let uniqueIds: string[] = [];
            for (let id of uniquifier) uniqueIds.push(id);
            if (uniqueIds.length === 1) return null;

            let metas = await mapInParallel(uniqueIds, (id) => ControlPointMetadata.fromId(host.app, id));
            if (metas.indexOf(null) !== -1) return null;

            let schemas = await mapInParallel(metas, (meta) => meta.point.fetchSchema());
            sourceArr   = metas.map((meta,
                                     idx) => new TimeSeriesSource(host, host.app.domain.units, meta, schemas[idx]));
        }

        return new TimeSeriesSourceTuple(host, host.app.domain.units, sourceArr[0], sourceArr[1], sourceArr[2], name);
    }

    private getSharedLocation(xLocation: string,
                              yLocation: string,
                              zLocation?: string): string
    {
        let sharedLocation: string = undefined;
        if (xLocation === yLocation)
        {
            sharedLocation = xLocation;

            if (zLocation && zLocation !== xLocation)
            {
                sharedLocation = undefined;
            }
        }
        return sharedLocation;
    }

    async getDataSource(property: string,
                        includeAlerts: boolean,
                        start: moment.Moment,
                        end: moment.Moment,
                        units: ScatterPlotPropertyTuple<Models.EngineeringUnitsFactors>,
                        timeOffset?: Models.TimeDuration): Promise<DataSourceTuple>
    {
        let xMeta = this.sourceX.meta;
        let yMeta = this.sourceY.meta;
        let zMeta = this.sourceZ && this.sourceZ.meta || undefined;

        let unitsDescriptorTasks: Promise<EngineeringUnitsDescriptorExtended>[] = [
            units.valueX ? this.unitsSvc.resolveDescriptor(units.valueX, false) : null,
            units.valueY ? this.unitsSvc.resolveDescriptor(units.valueY, false) : null
        ];

        let dataSourceTasks = [];

        let commonParameters: TimeSeriesSourceParameters = {
            property     : property,
            units        : undefined,
            rangeStart   : start,
            rangeEnd     : end,
            timeOffset   : timeOffset,
            includeAlerts: includeAlerts
        };

        dataSourceTasks.push(xMeta.point.getDataSourceForProperty(this.host.comp,
                                                                  {
                                                                      ...commonParameters,
                                                                      units: units.valueX
                                                                  }));

        dataSourceTasks.push(yMeta.point.getDataSourceForProperty(this.host.comp,
                                                                  {
                                                                      ...commonParameters,
                                                                      units: units.valueY
                                                                  }));

        let subSources = new ScatterPlotPropertyTuple<ScatterPlotSubSource>(0,
                                                                            new ScatterPlotSubSource(xMeta.name, xMeta.locationName),
                                                                            new ScatterPlotSubSource(yMeta.name, yMeta.locationName));

        if (zMeta)
        {
            unitsDescriptorTasks.push(units.valueZ ? this.unitsSvc.resolveDescriptor(units.valueZ, false) : null);

            dataSourceTasks.push(zMeta.point.getDataSourceForProperty(this.host.comp,
                                                                      {
                                                                          ...commonParameters,
                                                                          units: units.valueZ
                                                                      }));

            subSources.valueZ = new ScatterPlotSubSource(zMeta.name, zMeta.locationName);
        }

        let dataSourcesArr = await Promise.all(dataSourceTasks);

        let dataSources = new ScatterPlotPropertyTuple<ChartPointSource<any>>(0, dataSourcesArr[0], dataSourcesArr[1], dataSourcesArr[2] || undefined);

        let deviceElemExts = new ScatterPlotPropertyTuple<DeviceElementExtended>(0, xMeta.point, yMeta.point, zMeta?.point);

        let sharedLocation: string = this.getSharedLocation(xMeta.locationName, yMeta.locationName, zMeta?.locationName);
        let name                   = this.name;

        let formatter = {
            getTooltip(point: ScatterPlotPoint): string
            {
                let timestamp = point.getProcessedTimestamp();
                let res       = ChartPointSource.generateTooltipEntry("Timestamp", timestamp);
                if (name) res += ChartPointSource.generateTooltipEntry("Name", name);
                if (sharedLocation) res += ChartPointSource.generateTooltipEntry("Location", sharedLocation);

                res += ChartPointSource.generateTooltipEntry(`(X) ${xMeta.name}`, dataSources.valueX.getDisplayValue(point.value.valueX));
                res += ChartPointSource.generateTooltipEntry(`(Y) ${yMeta.name}`, dataSources.valueY.getDisplayValue(point.value.valueY));
                if (dataSources.valueZ) res += ChartPointSource.generateTooltipEntry(`(Color) ${zMeta.name}`, dataSources.valueZ.getDisplayValue(point.value.valueZ), false);

                return res;
            },
            getTooltipText(point: ScatterPlotPoint): string
            {
                let timestamp = point.getProcessedTimestamp();
                let text      = "";

                if (timestamp) text += ChartPointSource.generateTooltipEntryText("Timestamp", timestamp);
                if (name) text += ChartPointSource.generateTooltipEntryText("Name", name);
                if (sharedLocation) text += ChartPointSource.generateTooltipEntryText("Location", sharedLocation);

                text += ChartPointSource.generateTooltipEntryText(`(X) ${xMeta.name}`, dataSources.valueX.getDisplayValue(point.value.valueX));
                text += ChartPointSource.generateTooltipEntryText(`(Y) ${yMeta.name}`, dataSources.valueY.getDisplayValue(point.value.valueY));
                if (dataSources.valueZ) text += ChartPointSource.generateTooltipEntryText(`(Color) ${zMeta.name}`, dataSources.valueZ.getDisplayValue(point.value.valueZ), false);

                return text;
            }
        };

        let unitDescriptors = await Promise.all(unitsDescriptorTasks);
        this.setUnitsDetails(subSources.valueX, unitDescriptors[0]);
        this.setUnitsDetails(subSources.valueY, unitDescriptors[1]);
        this.setUnitsDetails(subSources.valueZ, unitDescriptors[2]);

        let source = new DataSourceTuple(this.host.app, dataSources, subSources, formatter);

        source.label    = property;
        source.provider = new ChartPointTupleProviderImpl(this.host.comp, deviceElemExts, property, this.host.comp.app.domain.assets, source, start, end, timeOffset, undefined, units);

        return source;
    }

    private setUnitsDetails(tuple: ScatterPlotSubSource,
                            ext: EngineeringUnitsDescriptorExtended)
    {
        if (tuple)
        {
            tuple.labelGenerator = (l) => ext?.generateLabel(l) || "";
            tuple.unitsDisplay   = ext?.model.displayName || "";
        }
    }
}
