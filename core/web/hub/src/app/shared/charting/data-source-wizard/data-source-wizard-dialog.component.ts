import {ChangeDetectionStrategy, Component, Inject, Injector} from "@angular/core";

import {AppContext} from "app/app.service";
import {TimeSeriesChartConfigurationExtended} from "app/customer/visualization/time-series-utils";
import {AssetGraphExtended, AssetGraphHierarchyTupleResult, AssetGraphTreeNode, SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {AssetExtended, DeviceElementExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {GraphConfigurationHost, GraphConfigurationHostChecker} from "app/shared/assets/configuration/graph-configuration-host";
import {ConditionNode, ConditionNodeType} from "app/shared/assets/tag-condition-builder/tag-conditions";
import {PivotTable, PivotTableView, VirtualAssetGraphNodeExtended} from "app/shared/charting/interactive-tree/interactive-tree.component";
import {ScatterPlotSourceTupleExtended} from "app/shared/charting/scatter-plot/scatter-plot-container.component";
import {WizardDialogComponent, WizardDialogState} from "app/shared/overlays/wizard-dialog.component";

import {UtilsService} from "framework/services/utils.service";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

import {Subject} from "rxjs";

@Component({
               templateUrl    : "./data-source-wizard-dialog.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class DataSourceWizardDialogComponent extends WizardDialogComponent<DataSourceWizardState>
{
    constructor(public dialogRef: OverlayDialogRef<boolean>,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: DataSourceWizardState)
    {
        super(dialogRef, inj, data);
    }

    public static open(cfg: WizardDialogState,
                       base: BaseApplicationComponent): Promise<boolean>
    {
        return super.open(cfg, base, DataSourceWizardDialogComponent);
    }


    public ngOnDestroy()
    {
        super.ngOnDestroy();

        this.data.cleanUp();
    }
}

export enum DataSourceWizardPurpose
{
    dashboard     = "dashboard",
    pane          = "pane",
    report        = "report",
    visualization = "visualization"
}

export class DataSourceWizardState extends WizardDialogState
{
    hasGps: boolean;

    typeChanged = new Subject<Models.TimeSeriesChartType>();

    private m_type: Models.TimeSeriesChartType;
    get type(): Models.TimeSeriesChartType
    {
        return this.m_type;
    }

    set type(type: Models.TimeSeriesChartType)
    {
        this.m_type = type;
        this.typeChanged.next(type);
    }

    overrideType: Models.TimeSeriesChartType;
    disabledTypes: Models.TimeSeriesChartType[] = [];

    panels: Models.TimeSeriesPanelConfiguration[] = [];

    ids: Models.RecordIdentity[]                  = [];
    sourceTuples: Models.ScatterPlotSourceTuple[] = [];

    locallyBound: boolean;
    localGraph: Models.AssetGraph;
    hierarchy: Models.HierarchicalVisualization = TimeSeriesChartConfigurationExtended.emptyHierarchy();
    pivot: PivotTable;
    pivotTable: PivotTableView;

    graphBindings: Models.AssetGraphBinding[]           = [];
    initialExternalBindings: Models.AssetGraphBinding[] = [];

    graphsResolved = new Subject<void>();

    private m_graphsChecker: GraphConfigurationHostChecker;
    get graphsChecker(): GraphConfigurationHostChecker
    {
        return this.m_graphsChecker;
    }

    private m_graphsHost: GraphConfigurationHost;
    get graphsHost(): GraphConfigurationHost
    {
        return this.m_graphsHost;
    }

    set graphsHost(graphsHost: GraphConfigurationHost)
    {
        this.m_graphsChecker?.cleanUp();
        this.m_graphsHost = graphsHost;
        if (graphsHost)
        {
            this.m_graphsChecker = new GraphConfigurationHostChecker(graphsHost, this.graphsResolved);
        }
    }

    private m_externalGraphsChecker: GraphConfigurationHostChecker;
    get externalGraphsChecker(): GraphConfigurationHostChecker
    {
        return this.m_externalGraphsChecker;
    }

    private m_externalGraphsHost: GraphConfigurationHost;
    get externalGraphsHost(): GraphConfigurationHost
    {
        return this.m_externalGraphsHost;
    }

    set externalGraphsHost(graphsHost: GraphConfigurationHost)
    {
        this.m_externalGraphsChecker?.cleanUp();
        this.m_externalGraphsHost = graphsHost;
        if (graphsHost)
        {
            this.m_externalGraphsChecker = new GraphConfigurationHostChecker(graphsHost, this.graphsResolved);
        }
    }

    get graphSelection(): string
    {
        return AssetGraphTreeNode.getIdFromBinding(this.graphBinding);
    }

    set graphSelection(id: string)
    {
        this.graphBinding = AssetGraphTreeNode.getBinding(id);
    }

    private m_graphSelections: string[];
    get graphSelections(): string[]
    {
        if (!this.m_graphSelections)
        {
            this.m_graphSelections = this.graphBindings.map((b) => AssetGraphTreeNode.getIdFromBinding(b));
        }

        return this.m_graphSelections;
    }

    set graphSelections(ids: string[])
    {
        this.m_graphSelections = ids || [];
        this.graphBindings     = this.m_graphSelections.map((id) => AssetGraphTreeNode.getBinding(id));
        this.initializeLocalGraphSelections();
        this.initializeExternalGraphSelections();
    }

    private m_localGraphSelections: string[];
    get localGraphSelections(): string[]
    {
        if (!this.m_localGraphSelections) this.initializeLocalGraphSelections();

        return this.m_localGraphSelections;
    }

    set localGraphSelections(ids: string[])
    {
        this.m_localGraphSelections = ids || [];
        this.rebuildGraphBindings();

        if (this.type === Models.TimeSeriesChartType.HIERARCHICAL)
        {
            const leafNodeIds       = this.graphBindings.map((binding) => binding.nodeId);
            const selections        = new Set(leafNodeIds);
            const existingBindings  = new Set<string>();
            this.hierarchy.bindings = this.hierarchy.bindings.filter((binding) =>
                                                                     {
                                                                         if (selections.has(binding.leafNodeId))
                                                                         {
                                                                             existingBindings.add(binding.leafNodeId);
                                                                             return true;
                                                                         }
                                                                         return false;
                                                                     });
            for (let nodeId of selections)
            {
                if (!existingBindings.has(nodeId))
                {
                    this.hierarchy.bindings.push(Models.HierarchicalVisualizationBinding.newInstance({leafNodeId: nodeId}));
                }
            }
        }
    }

    private m_externalGraphSelections: string[];
    get externalGraphSelections(): string[]
    {
        if (!this.m_externalGraphSelections) this.initializeExternalGraphSelections();

        return this.m_externalGraphSelections;
    }

    set externalGraphSelections(ids: string[])
    {
        this.m_externalGraphSelections = ids || [];
        this.rebuildGraphBindings();
    }

    graphBinding: Models.AssetGraphBinding;
    newSelectorName: string;

    get selectorId(): string
    {
        return this.graphBinding?.selectorId || null;
    }

    set selectorId(selectorId: string)
    {
        if (this.graphBinding)
        {
            this.graphBinding.selectorId = selectorId;
        }
    }

    constructor(isNew: boolean,
                public readonly purpose: DataSourceWizardPurpose,
                graphsHost: GraphConfigurationHost,
                externalGraphsHost: GraphConfigurationHost,
                public readonly singleSelect: boolean)
    {
        super(isNew);

        this.graphsHost         = graphsHost;
        this.externalGraphsHost = externalGraphsHost;
    }

    private initializeLocalGraphSelections()
    {
        let graphLookup             = UtilsService.extractLookup(this.graphsHost.getGraphs());
        this.m_localGraphSelections = this.graphBindings.filter((b) => graphLookup[b.graphId])
                                          .map((b) => AssetGraphTreeNode.getIdFromBinding(b));
    }

    private initializeExternalGraphSelections()
    {
        let graphLookup                = UtilsService.extractLookup(this.externalGraphsHost?.getGraphs() || []);
        this.m_externalGraphSelections = this.graphBindings.filter((b) => graphLookup[b.graphId])
                                             .map((b) => AssetGraphTreeNode.getIdFromBinding(b));
    }

    private rebuildGraphBindings()
    {
        this.graphBindings = [];
        if (this.graphsChecker?.isValid)
        {
            for (let localId of this.localGraphSelections) this.graphBindings.push(AssetGraphTreeNode.getBinding(localId));
        }
        if (this.externalGraphsChecker?.isValid)
        {
            for (let externalId of this.externalGraphSelections) this.graphBindings.push(AssetGraphTreeNode.getBinding(externalId));
        }
    }

    hasSelection(): boolean
    {
        if (this.overrideType) return true;

        switch (this.type)
        {
            case Models.TimeSeriesChartType.STANDARD:
            case Models.TimeSeriesChartType.COORDINATE:
                return this.ids?.length > 0;

            case Models.TimeSeriesChartType.HIERARCHICAL:
                return !!this.hierarchy?.bindings?.length && !!this.localGraph;

            case Models.TimeSeriesChartType.SCATTER:
            case Models.TimeSeriesChartType.GRAPH_SCATTER:
                return this.sourceTuples?.length > 0;

            case Models.TimeSeriesChartType.GRAPH:
                if (GraphConfigurationHostChecker.isValid(this.graphsHost)) return true;
                if (GraphConfigurationHostChecker.isValid(this.externalGraphsHost)) return true;
                if (this.graphBinding) return true;
                if (this.graphBindings.length) return true;
                break;
        }

        return false;
    }

    async rebuildPivotTable(app: AppContext,
                            nodes?: Models.VirtualAssetGraphNode[])
    {
        if (this.purpose === DataSourceWizardPurpose.visualization && this.type === Models.TimeSeriesChartType.HIERARCHICAL)
        {
            this.pivotTable = null;

            if (nodes) this.hierarchy.virtualNodes = nodes;

            // Evaluate the current query fully
            const graphExt    = new AssetGraphExtended(app.domain, this.localGraph);
            const response    = await graphExt.resolve();
            const leafNodeIds = this.hierarchy.bindings.map((binding) => binding.leafNodeId);
            const bindings    = leafNodeIds.map((leafNodeId) => Models.AssetGraphBinding.newInstance({nodeId: leafNodeId}));
            const result      = response.resolveBindingTuples(bindings, true);
            if (!this.hierarchy.virtualNodes?.length)
            {
                this.hierarchy.virtualNodes = [];

                let nodeId = leafNodeIds[0];
                while (nodeId)
                {
                    let virtualNode = VirtualAssetGraphNodeExtended.newModel(nodeId, Models.VirtualAssetGraphNodeType.Name);
                    if (this.hierarchy.virtualNodes.length === 0 && leafNodeIds.length > 1) virtualNode.nodeId = null;
                    this.hierarchy.virtualNodes.unshift(virtualNode);

                    nodeId = graphExt.getNodeParentId(nodeId);
                }
            }

            // Create a pivot table
            let uniqueResults = new Map<string, AssetGraphHierarchyTupleResult>();
            this.pivot        = new PivotTable(result, this.hierarchy.virtualNodes, graphExt, leafNodeIds);

            // Load assets for the table
            await this.pivot.load(app);

            // Create preview of the table and get column options
            await this.constructPivotTable();
        }
    }

    private async constructPivotTable()
    {
        let hierarchicalBindings = this.hierarchy.bindings.map((binding) => Models.HierarchicalVisualizationBinding.newInstance({leafNodeId: binding.leafNodeId}));
        this.pivotTable          = await PivotTableView.new(this.pivot, hierarchicalBindings);
    }

    async updatePivotTable()
    {
        if (this.pivotTable && this.hierarchy.virtualNodes.length)
        {
            await this.constructPivotTable();
        }
    }

    cleanUp()
    {
        this.m_graphsChecker?.cleanUp();
        this.m_externalGraphsChecker?.cleanUp();
    }

    //--//

    async create(comp: BaseApplicationComponent,
                 goto: boolean): Promise<boolean>
    {
        return true;
    }

    async save(comp: BaseApplicationComponent): Promise<boolean>
    {
        return true;
    }

    async load(comp: BaseApplicationComponent): Promise<boolean>
    {
        if (this.purpose === DataSourceWizardPurpose.visualization)
        {
            let gpsClassId = await comp.app.domain.normalization.getWellKnownEquipmentClassId(Models.WellKnownEquipmentClass.GPS);
            if (gpsClassId)
            {
                let tag      = new ConditionNode(ConditionNodeType.EQUIPMENT, false);
                tag.value    = gpsClassId;
                let response = await comp.app.domain.assets.getList(Models.AssetFilterRequest.newInstance({tagsQuery: tag.toModel()}));
                this.hasGps  = response.results.length > 0;
            }
        }

        return true;
    }

    //--//

    updateForControlPointsGroup(model: Models.ControlPointsGroup)
    {
        this.type = Models.TimeSeriesChartType.GRAPH;
        if (model.graph)
        {
            this.locallyBound = true;
            this.localGraph   = model.graph;
            const binding     = Models.HierarchicalVisualizationBinding.newInstance({leafNodeId: model.pointInput.nodeId});
            this.hierarchy    = Models.HierarchicalVisualization.newInstance({bindings: [binding]});
            this.graphBinding = Models.AssetGraphBinding.newInstance({
                                                                         graphId: SharedAssetGraphExtended.LOCAL_GRAPH_ID,
                                                                         nodeId : binding.leafNodeId
                                                                     });
        }
        else if (model.pointInput)
        {
            this.locallyBound = false;
            this.graphBinding = model.pointInput;
            this.ids          = model.selections.identities;
        }
        else
        {
            this.type = Models.TimeSeriesChartType.STANDARD;
            this.ids  = model.selections.identities;
        }
    }

    updateForControlPointWidget(config?: Models.ControlPointWidgetConfiguration)
    {
        this.type = Models.TimeSeriesChartType.STANDARD;

        if (config)
        {
            if (config.pointInput)
            {
                this.type         = Models.TimeSeriesChartType.GRAPH;
                this.graphBinding = config.pointInput;
            }
            else if (config.pointId)
            {
                this.ids = [AssetExtended.newIdentityRaw(config.pointId)];
            }
        }
    }

    async updateForChart(chart: TimeSeriesChartConfigurationExtended)
    {
        let model = chart.model;
        this.type = model.type;

        switch (this.type)
        {
            case Models.TimeSeriesChartType.STANDARD:
                if (this.overrideType === Models.TimeSeriesChartType.GRAPH) this.updateForGraphLine(chart);
            // fall through

            case Models.TimeSeriesChartType.COORDINATE:
                this.updateForStandardLine(chart);
                break;

            case Models.TimeSeriesChartType.HIERARCHICAL:
                let sharedGraph = model.graph?.sharedGraphs?.[0];
                this.hierarchy  = Models.HierarchicalVisualization.deepClone(model.hierarchy) || TimeSeriesChartConfigurationExtended.emptyHierarchy();
                this.localGraph = sharedGraph?.graph || null;
                if (this.hierarchy.bindings?.length && sharedGraph)
                {
                    this.graphBindings = this.hierarchy.bindings.map((binding) => Models.AssetGraphBinding.newInstance({
                                                                                                                           graphId: sharedGraph.id,
                                                                                                                           nodeId : binding.leafNodeId
                                                                                                                       }));
                }
                await this.rebuildPivotTable(chart.app);
                break;

            case Models.TimeSeriesChartType.GRAPH:
                this.updateForGraphLine(chart);

                if (this.overrideType === Models.TimeSeriesChartType.STANDARD) this.updateForStandardLine(chart);
                break;

            case Models.TimeSeriesChartType.SCATTER:
                this.ids = [];
                for (let sourceTuple of model.scatterPlot.sourceTuples)
                {
                    let addSource = (sourceId: string) =>
                    {
                        if (sourceId && !this.ids.find((recordId) => recordId.sysId === sourceId))
                        {
                            this.ids.push(DeviceElementExtended.newIdentity(sourceId));
                        }
                    };

                    if (sourceTuple.sourceX) addSource(sourceTuple.sourceX.deviceElementId);
                    if (sourceTuple.sourceY) addSource(sourceTuple.sourceY.deviceElementId);
                    if (sourceTuple.sourceZ) addSource(sourceTuple.sourceZ.deviceElementId);
                }
            // fall through to get other scatter set up

            case Models.TimeSeriesChartType.GRAPH_SCATTER:
                this.panels       = model.panels;
                this.sourceTuples = model.scatterPlot.sourceTuples.map((sourceTuple) => ScatterPlotSourceTupleExtended.deepCopy(sourceTuple));
                if (this.sourceTuples[0]?.panel == null)
                {
                    for (let i = 0; i < this.sourceTuples.length; i++) this.sourceTuples[i].panel = i;
                }
                if (this.type === Models.TimeSeriesChartType.SCATTER) break;
                break;
        }
    }

    private updateForGraphLine(chart: TimeSeriesChartConfigurationExtended)
    {
        let selections = new Map<string, Models.AssetGraphBinding>();
        for (let source of chart.dataSources)
        {
            let binding = AssetGraphTreeNode.getIdFromBinding(source.pointBinding);
            if (binding) selections.set(binding, source.pointBinding);
        }

        this.graphBindings             = [...selections.values()];
        this.initialExternalBindings   = this.graphBindings.filter((binding) => chart.model.graph.externalBindings.some((external) => external.graphId === binding.graphId));
        this.m_externalGraphSelections = this.initialExternalBindings.map((binding) => AssetGraphTreeNode.getIdFromBinding(binding));
    }

    private updateForStandardLine(chart: TimeSeriesChartConfigurationExtended)
    {
        this.ids = chart.sourcesExt
                        .filter((sourceExt) => !sourceExt.markedForDeletion && !sourceExt.model.pointBinding)
                        .map((sourceExt) => DeviceElementExtended.newIdentity(sourceExt.model.id));
    }
}
