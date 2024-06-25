import {Component, Inject, Injector, Type} from "@angular/core";

import {AppContext} from "app/app.service";
import {WidgetManipulator} from "app/dashboard/dashboard/widgets/widget-manipulator";
import {AlertDefinitionExtended} from "app/services/domain/alert-definitions.service";
import {SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {LocationExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {DashboardConfigurationExtended} from "app/services/domain/dashboard-management.service";
import {WidgetConfigurationExtended} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {DataAggregationExtended, DataAggregationType} from "app/shared/aggregation/data-aggregation.component";
import {GraphConfigurationHost, GraphConfigurationHostChecker} from "app/shared/assets/configuration/graph-configuration-host";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";
import {WizardDialogComponent, WizardDialogState} from "app/shared/overlays/wizard-dialog.component";
import {ALL_ENUMS_ID} from "app/shared/selection/enum-selection";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {VerticalViewWindow} from "framework/ui/charting/vertical-view-window";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {mapInParallel} from "framework/utils/concurrency";

import {Subject, Subscription} from "rxjs";

@Component({
               templateUrl: "./widget-editor-wizard-dialog.component.html"
           })
export class WidgetEditorWizardDialogComponent extends WizardDialogComponent<WidgetEditorWizardState>
{
    public viewWindow: VerticalViewWindow = null;

    constructor(public dialogRef: OverlayDialogRef<boolean>,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: WidgetEditorWizardState)
    {
        super(dialogRef, inj, data);
    }

    public static async open(cfg: WizardDialogState): Promise<boolean>
    {
        let widgetWizardCfg = <WidgetEditorWizardState>cfg;
        widgetWizardCfg.host.app.domain.dashboard.widgetWizardOpened();

        try
        {
            // trigger wizard experience: returns true if a change was made
            return await super.open(widgetWizardCfg, widgetWizardCfg.host, WidgetEditorWizardDialogComponent);
        }
        finally
        {
            widgetWizardCfg.host.app.domain.dashboard.widgetWizardClosed();
        }
    }
}

export class WidgetEditorWizardState extends WizardDialogState
{
    editor: WidgetEditorData;

    get app(): AppContext
    {
        return this.host.app;
    }

    get widgetOutline(): Models.WidgetOutline
    {
        return Models.WidgetOutline.newInstance(this.m_widget?.outline);
    }

    constructor(public readonly host: BaseApplicationComponent,
                private m_widget: Models.WidgetComposition,
                parentManipulator: WidgetManipulator,
                forSubgroup: boolean,
                startingStep?: string)
    {
        super(!m_widget, startingStep);

        this.editor = new WidgetEditorData(this.app, parentManipulator, forSubgroup);
    }

    public async create(comp: BaseApplicationComponent,
                        goto: boolean): Promise<boolean>
    {
        // We leave it up to the caller of the dialog to handle the creation implementation
        return true;
    }

    public async load(comp: BaseApplicationComponent): Promise<boolean>
    {
        try
        {
            await this.editor.init(this.app.domain.dashboard.currentDashboardConfig.getValue(), Models.WidgetConfiguration.deepClone(this.m_widget?.config));
            return true;
        }
        catch (e)
        {
            return false;
        }
    }

    public async save(comp: BaseApplicationComponent): Promise<boolean>
    {
        // We leave it up to the caller of the dialog to handle the creation implementation
        return true;
    }

    cleanUp()
    {
        super.cleanUp();

        this.editor.dashboardGraphsChecker?.cleanUp();
        this.editor.localGraphsChecker?.cleanUp();
        if (this.editor.localGraphsListener) this.editor.localGraphsListener.unsubscribe();
    }
}

class WidgetEditorData
{
    id: string;
    type: string;
    buildings: Models.LocationHierarchy[]                     = [];
    dashboardExt: DashboardConfigurationExtended;
    newSelectorNameLookup: Lookup<Models.SharedAssetSelector> = {};

    private m_dashboardGraphsChecker: GraphConfigurationHostChecker;
    get dashboardGraphsChecker(): GraphConfigurationHostChecker
    {
        return this.m_dashboardGraphsChecker;
    }

    graphLookup: Map<string, SharedAssetGraphExtended>;

    private m_dashboardGraphsHost: GraphConfigurationHost;
    get dashboardGraphsHost(): GraphConfigurationHost
    {
        return this.m_dashboardGraphsHost;
    }

    set dashboardGraphsHost(graphsHost: GraphConfigurationHost)
    {
        this.m_dashboardGraphsChecker?.cleanUp();
        this.m_dashboardGraphsHost = graphsHost;
        if (this.m_dashboardGraphsHost)
        {
            this.m_dashboardGraphsChecker = new GraphConfigurationHostChecker(this.m_dashboardGraphsHost);
        }
    }

    private m_localGraphsChecker: GraphConfigurationHostChecker;
    get localGraphsChecker(): GraphConfigurationHostChecker
    {
        return this.m_localGraphsChecker;
    }

    localGraphsListener: Subscription;
    private m_localGraphsHost: GraphConfigurationHost;
    get localGraphsHost(): GraphConfigurationHost
    {
        return this.m_localGraphsHost;
    }

    set localGraphsHost(graphsHost: GraphConfigurationHost)
    {
        this.m_localGraphsChecker?.cleanUp();
        this.m_localGraphsHost = graphsHost;
        if (this.m_localGraphsHost)
        {
            this.m_localGraphsChecker = new GraphConfigurationHostChecker(this.m_localGraphsHost);
        }
    }

    private m_widget: Models.WidgetConfiguration;
    public get widget(): Models.WidgetConfiguration
    {
        return this.m_widget;
    }

    public set widget(value: Models.WidgetConfiguration)
    {
        this.m_widget = value;

        if (value instanceof Models.AggregationTableWidgetConfiguration ||
            value instanceof Models.AggregationTrendWidgetConfiguration)
        {
            this.controlPointGroups = value.groups || [];
        }

        if (this.widget instanceof Models.AggregationWidgetConfiguration)
        {
            if (!this.widget.controlPointGroup) this.widget.controlPointGroup = new Models.ControlPointsGroup();

            this.controlPointGroups = [this.widget.controlPointGroup];
        }
    }

    controlPointGroups: Models.ControlPointsGroup[] = [];

    selectedCategories: Lookup<boolean>  = {};
    selectedStates: Lookup<boolean>      = {};
    selectedAlertStatus: Lookup<boolean> = {};
    selectedAlertType: Lookup<boolean>   = {};
    selectedAlertRule: Lookup<boolean>   = {};
    selectedLocations: string[]          = [];

    timeRange: Models.RangeSelection;

    selectedRanges: Models.FilterableTimeRange[];
    controlPointDisplayType: Models.ControlPointDisplayType = Models.ControlPointDisplayType.NameOnly;
    visualizationLegend: boolean                            = false;
    visualizationRanges: boolean                            = false;
    compareBetweenGroups: boolean                           = true;
    dataAggregationExt: DataAggregationExtended;

    aggregationTrendVisualizationMode: Models.AggregationTrendVisualizationMode = Models.AggregationTrendVisualizationMode.Line;
    aggregationTrendShowYAxis: boolean                                          = true;
    aggregationTrendShowLegend: boolean                                         = true;

    timeSeriesCharts: Models.TimeSeriesChartConfiguration[] = [];
    timeseriesRange: Models.RangeSelection                  = RangeSelectionExtended.newModel(Models.TimeRangeId.Last24Hours);
    externalGraphs: Models.TimeSeriesGraphConfiguration[]   = [];
    validExternalGpsBindings: Models.AssetGraphBinding[]    = [];

    constructor(public app: AppContext,
                public readonly parentManipulator: WidgetManipulator,
                public readonly forSubgroup: boolean)
    {
    }

    async init(containingDashboard: DashboardConfigurationExtended,
               config: Models.WidgetConfiguration): Promise<void>
    {
        if (!containingDashboard) throw new Error("No containing dashboard provided");

        this.buildings = await this.app.domain.locations.getLocationHierarchy();

        this.dashboardExt        = containingDashboard.cloneForEdit();
        this.dashboardGraphsHost = this.dashboardExt.graphConfigurationHost;
        await this.dashboardExt.resolveGraphs();

        try
        {
            let ext     = WidgetConfigurationExtended.fromConfigModel(config);
            this.widget = ext.model;
            this.type   = ext.getDescriptor().config.typeName;
        }
        catch (err)
        {
            let ext   = WidgetConfigurationExtended.fromConfigModel(new Models.AggregationWidgetConfiguration());
            this.type = ext.getDescriptor().config.typeName;
            this.configure();
        }

        // Initialize for editing from model state
        this.initWidgetSelections();
        this.initDeviceWidgetSelections();
        this.initAlertWidgetSelections();
        this.initAggregation();
        this.initTables();
        this.initAggregationTrend();
        await this.initTimeseries();
    }

    initWidgetSelections()
    {
        if (this.widget.locations)
        {
            this.selectedLocations = this.widget.locations;
        }
    }

    initDeviceWidgetSelections()
    {
        if (this.widget instanceof Models.DeviceWidgetConfiguration)
        {
            let deviceConfiguration = this.widget;
            if (deviceConfiguration.categories?.length)
            {
                for (let category of deviceConfiguration.categories) this.selectedCategories[category] = true;
            }
            else
            {
                this.selectedCategories[ALL_ENUMS_ID] = true;
            }

            if (deviceConfiguration.states?.length)
            {
                for (let state of deviceConfiguration.states) this.selectedStates[state] = true;
            }
            else
            {
                this.selectedStates[ALL_ENUMS_ID] = true;
            }
        }
    }

    initAlertWidgetSelections()
    {
        if (this.widget instanceof Models.AlertWidgetConfiguration)
        {
            this.initAlertType(this.widget.alertTypes);
        }

        if (this.widget instanceof Models.AlertTableWidgetConfiguration)
        {
            this.initAlertStatus(this.widget.alertStatusIDs);
            this.initAlertType(this.widget.alertTypeIDs);
            this.initAlertRule(this.widget.alertRules);
        }

        if (this.widget instanceof Models.AlertFeedWidgetConfiguration)
        {
            if (!this.widget.timeRange)
            {
                this.widget.timeRange = RangeSelectionExtended.newModel(Models.TimeRangeId.Last30Days);
            }

            this.timeRange = this.widget.timeRange;
        }
    }

    initAlertStatus(values: Models.AlertStatus[])
    {
        if (values?.length)
        {
            for (let alertType of values) this.selectedAlertStatus[alertType] = true;
        }
        else
        {
            this.selectedAlertStatus[ALL_ENUMS_ID] = true;
        }
    }

    initAlertType(values: Models.AlertType[])
    {
        this.selectedAlertType = {};

        if (values && values.length)
        {
            this.selectedAlertType[ALL_ENUMS_ID] = false;

            for (let alertType of values) this.selectedAlertType[alertType] = true;
        }
        else
        {
            this.selectedAlertType[ALL_ENUMS_ID] = true;
        }
    }

    initAlertRule(values: Models.RecordIdentity[])
    {
        this.selectedAlertRule = {};

        if (values && values.length)
        {
            this.selectedAlertRule[ALL_ENUMS_ID] = false;

            for (let alertRule of values)
            {
                if (alertRule && alertRule.sysId)
                {
                    this.selectedAlertRule[alertRule.sysId] = true;
                }
            }
        }
        else
        {
            this.selectedAlertRule[ALL_ENUMS_ID] = true;
        }
    }

    initAggregation()
    {
        if (this.widget instanceof Models.AggregationWidgetConfiguration)
        {
            let filterableRange = this.widget.filterableRange;

            if (filterableRange) this.selectedRanges = [Models.FilterableTimeRange.deepClone(filterableRange)];
        }
    }

    initTables()
    {
        let aggregationTable = this.widget;
        if (aggregationTable instanceof Models.AggregationTableWidgetConfiguration)
        {
            this.controlPointDisplayType = aggregationTable.controlPointDisplayType;
            this.compareBetweenGroups    = !aggregationTable.isolateGroupRanges;
            this.visualizationLegend     = aggregationTable.visualizationLegend;
            this.visualizationRanges     = aggregationTable.visualizationRanges;
            if (aggregationTable.filterableRanges)
            {
                this.selectedRanges = aggregationTable.filterableRanges.map((filterableRange) => Models.FilterableTimeRange.deepClone(filterableRange));
            }

            this.dataAggregationExt = new DataAggregationExtended(aggregationTable, false);
            this.initBindingsTable();
        }

        if (this.widget instanceof Models.AlertTableWidgetConfiguration && this.widget.filterableRanges)
        {
            this.selectedRanges = this.widget.filterableRanges.map((filterableRange) => Models.FilterableTimeRange.deepClone(filterableRange));
        }
    }

    initBindingsTable()
    {
        if (this.localGraphsListener)
        {
            this.localGraphsListener.unsubscribe();
            this.localGraphsListener = null;
        }

        if (this.widget instanceof Models.AggregationTableWidgetConfiguration && this.dataAggregationExt.type === DataAggregationType.Bindings)
        {
            const graphs: Models.SharedAssetGraph[] = [SharedAssetGraphExtended.newModel(this.widget.graph, null, "Asset Graph")];
            const changedSubject                    = new Subject<void>();
            this.localGraphsHost                    = {
                hostContext  : "Data Aggregation",
                graphsChanged: changedSubject,
                getGraphs    : () => graphs,
                resolveGraphs: () => SharedAssetGraphExtended.loadGraphs(this.app.domain, graphs),
                canRemove    : () => true,
                canRemoveNode: (graphId: string,
                                nodeId: string) =>
                {
                    if (this.widget instanceof Models.AggregationTableWidgetConfiguration && this.dataAggregationExt.type === DataAggregationType.Bindings)
                    {
                        return this.widget.columns.every((column) => column.nodeId !== nodeId);
                    }
                    return true;
                }
            };

            this.localGraphsListener = changedSubject.subscribe(() =>
                                                                {
                                                                    if (this.widget instanceof Models.AggregationTableWidgetConfiguration)
                                                                    {
                                                                        let graph = graphs[0]?.graph;
                                                                        if (!UtilsService.compareJson(this.widget.graph, graph))
                                                                        {
                                                                            let availableNodes = new Set(graph.nodes.map((node) => node.id));

                                                                            this.widget.graph   = graph;
                                                                            this.widget.columns = this.widget.columns.filter((col) => availableNodes.has(col.nodeId));
                                                                        }
                                                                    }
                                                                });
        }
    }

    initAggregationTrend()
    {
        if (this.widget instanceof Models.AggregationTrendWidgetConfiguration)
        {
            this.aggregationTrendVisualizationMode = this.widget.visualizationMode;
            this.aggregationTrendShowYAxis         = this.widget.showY;
            this.aggregationTrendShowLegend        = this.widget.showLegend;

            let filterableRange = this.widget.filterableRange;
            if (filterableRange) this.selectedRanges = [Models.FilterableTimeRange.deepClone(filterableRange)];
        }
    }

    async initTimeseries()
    {
        if (this.widget instanceof Models.TimeSeriesWidgetConfiguration)
        {
            if (this.widget.charts)
            {
                this.timeSeriesCharts = this.widget.charts.map((chart) => Models.TimeSeriesChartConfiguration.deepClone(chart));
                await this.updateExternalGraphs();
            }
            if (this.widget.range)
            {
                this.timeseriesRange = Models.RangeSelection.newInstance(this.widget.range);
            }
        }
    }

    async updateExternalGraphs()
    {
        let graphs          = this.dashboardGraphsHost.getGraphs();
        this.graphLookup    = await this.dashboardGraphsHost.resolveGraphs();
        this.externalGraphs = await mapInParallel(this.timeSeriesCharts, async (chart) =>
        {
            let graphIdToBinding: Lookup<Models.AssetGraphBinding> = {};
            for (let graph of graphs) graphIdToBinding[graph.id] = Models.AssetGraphBinding.newInstance({graphId: graph.id});
            for (let binding of chart.graph?.externalBindings || []) graphIdToBinding[binding.graphId] = binding;

            let externalGraph = Models.TimeSeriesGraphConfiguration.newInstance({
                                                                                    sharedGraphs: [],
                                                                                    contexts    : []
                                                                                });
            for (let graphId in graphIdToBinding)
            {
                let externalBinding = graphIdToBinding[graphId];
                let graphExt        = this.graphLookup.get(graphId);
                if (graphExt)
                {
                    externalGraph.sharedGraphs.push(graphExt.modelClone());

                    let externalContext   = Models.AssetGraphContextAsset.newInstance({
                                                                                          graphId: graphId,
                                                                                          nodeId : graphExt.getRootNodes()[0]?.id
                                                                                      });
                    let selectedContext   = <Models.AssetGraphContextAsset>await this.dashboardExt.getGraphContext(externalBinding.selectorId);
                    externalContext.sysId = selectedContext?.sysId;
                    if (!externalContext.sysId)
                    {
                        let graphOptions      = await this.dashboardExt.getGraphOptions(graphId);
                        externalContext.sysId = graphOptions[0]?.id;
                    }
                    externalGraph.contexts.push(externalContext);
                }
            }

            return externalGraph;
        });
    }

    configure()
    {
        let ext = WidgetConfigurationExtended.fromName(this.type);
        ext.initializeForWizard();

        let newWidget: Models.WidgetConfiguration = ext.model;

        // copy existing data to new configuration
        if (this.widget)
        {
            newWidget.id              = this.widget.id;
            newWidget.name            = this.widget.name;
            newWidget.description     = this.widget.description;
            newWidget.locations       = this.widget.locations;
            newWidget.toolbarBehavior = this.widget.toolbarBehavior;
        }
        else
        {
            // If sub-widget, hide toolbar by default
            if (this.forSubgroup)
            {
                newWidget.toolbarBehavior = Models.WidgetToolbarBehavior.AutoHide;
            }
            else
            {
                newWidget.toolbarBehavior = Models.WidgetToolbarBehavior.AlwaysShow;
            }
        }

        // update our configuration object
        this.widget = newWidget;

        // initialize selections
        this.initTables();
        this.initAlertWidgetSelections();
        this.initDeviceWidgetSelections();
        this.initAggregationTrend();

        this.syncDeviceWidgetConfig();
        this.syncAlertTypes();
        this.syncLocations();
        this.syncAggregationConfig();
        this.syncTableConfigs();
        this.syncAggregationTrendConfig();
        this.syncTimeseriesConfig();
    }

    syncDeviceWidgetConfig()
    {
        if (this.widget instanceof Models.DeviceWidgetConfiguration)
        {
            let deviceConfiguration        = this.widget;
            deviceConfiguration.categories = this.map(this.selectedCategories);
            deviceConfiguration.states     = this.map(this.selectedStates);
        }
    }

    syncAlertTypes()
    {
        if (this.widget instanceof Models.AlertWidgetConfiguration)
        {
            this.widget.alertTypes = this.fetchAlertType();
        }

        if (this.widget instanceof Models.AlertTableWidgetConfiguration)
        {
            this.widget.alertStatusIDs = this.fetchAlertStatus();
            this.widget.alertTypeIDs   = this.fetchAlertType();
            this.widget.alertRules     = this.fetchAlertRules();
        }

        if (this.widget instanceof Models.AlertFeedWidgetConfiguration)
        {
            this.widget.timeRange = this.timeRange;
        }
    }

    fetchAlertStatus(): Models.AlertStatus[]
    {
        let values = [];

        for (let i in this.selectedAlertStatus)
        {
            if (i == ALL_ENUMS_ID && this.selectedAlertStatus[ALL_ENUMS_ID]) break;

            if (this.selectedAlertStatus.hasOwnProperty(i) && this.selectedAlertStatus[i])
            {
                values.push(UtilsService.getEnumValue(Models.AlertStatus, i));
            }
        }

        return values;
    }

    fetchAlertType(): Models.AlertType[]
    {
        let values = [];

        if (!this.selectedAlertType[ALL_ENUMS_ID])
        {
            for (let i in this.selectedAlertType)
            {
                if (this.selectedAlertType[i])
                {
                    values.push(UtilsService.getEnumValue(Models.AlertType, i));
                }
            }
        }

        return values;
    }

    fetchAlertRules(): Models.RecordIdentity[]
    {
        let values = [];

        if (!this.selectedAlertRule[ALL_ENUMS_ID])
        {
            for (let i in this.selectedAlertRule)
            {
                if (this.selectedAlertRule[i])
                {
                    values.push(AlertDefinitionExtended.newIdentity(i));
                }
            }
        }

        return values;
    }

    async syncLocations()
    {
        if (this.widget)
        {
            this.widget.locations = this.selectedLocations;

            if (this.widget instanceof Models.AlertMapWidgetConfiguration)
            {
                if (this.selectedLocations?.length)
                {
                    this.widget.center = await this.getCenter(this.selectedLocations[0]);
                }
            }
        }
    }

    syncAggregationConfig()
    {
        if (this.widget instanceof Models.AggregationWidgetConfiguration)
        {
            if (this.selectedRanges?.length) this.widget.filterableRange = this.selectedRanges[0];
        }
    }

    syncAggregationTrendConfig()
    {
        if (this.widget instanceof Models.AggregationTrendWidgetConfiguration)
        {
            if (this.selectedRanges?.length) this.widget.filterableRange = this.selectedRanges[0];

            this.widget.visualizationMode = this.aggregationTrendVisualizationMode;
            this.widget.showY             = this.aggregationTrendShowYAxis;
            this.widget.showLegend        = this.aggregationTrendShowLegend;
        }
    }

    syncTableConfigs()
    {
        if (this.widget instanceof Models.AggregationTableWidgetConfiguration)
        {
            this.widget.controlPointDisplayType = this.controlPointDisplayType;
            this.widget.visualizationLegend     = this.visualizationLegend;
            this.widget.visualizationRanges     = this.visualizationRanges;
            this.widget.isolateGroupRanges      = !this.compareBetweenGroups;

            if (this.selectedRanges?.length) this.widget.filterableRanges = this.selectedRanges;
        }

        if (this.widget instanceof Models.AlertTableWidgetConfiguration)
        {
            this.widget.filterableRanges = this.selectedRanges;
        }
    }

    syncTimeseriesConfig()
    {
        if (this.widget instanceof Models.TimeSeriesWidgetConfiguration)
        {
            this.widget.charts = this.timeSeriesCharts.map((chart) => Models.TimeSeriesChartConfiguration.deepClone(chart));
            this.widget.range  = Models.RangeSelection.newInstance(this.timeseriesRange);
        }
    }

    readSelectionData(map: Lookup<boolean>): Set<string>
    {
        let set = new Set<string>();

        // Load selection from map
        for (let key in map)
        {
            if (map[key]) set.add(key);
        }

        return set;
    }

    writeSelectionData(set: Set<string>): Lookup<boolean>
    {
        let map: Lookup<boolean> = {};

        // Load map from selection
        for (let entry of set)
        {
            map[entry] = true;
        }

        return map;
    }

    allowWidgetTypes(...types: Type<Models.WidgetConfiguration>[]): boolean
    {
        return types.some((type) => this.widget instanceof type);
    }

    private map(selections: Lookup<boolean>): string[]
    {
        let array = [];

        for (let i in selections)
        {
            if (i == ALL_ENUMS_ID && selections[ALL_ENUMS_ID]) break;
            if (selections.hasOwnProperty(i) && selections[i]) array.push(i);
        }

        return array;
    }

    private first(selections: Lookup<boolean>): string
    {
        let array = [];

        for (let i in selections)
        {
            if (i == ALL_ENUMS_ID && selections[ALL_ENUMS_ID]) break;
            if (selections.hasOwnProperty(i) && selections[i]) array.push(i);
        }

        if (array.length) return array[0];
        return null;
    }

    private async getCenter(locationID: string): Promise<string>
    {
        let center: string                     = "United States of America";
        let selected: Models.LocationHierarchy = null;

        if (locationID)
        {
            selected = this.buildings.find((b) => b.ri.sysId == locationID);
        }

        if (!selected && this.buildings.length)
        {
            selected = this.buildings[0];
        }

        if (selected)
        {
            let location = await this.app.domain.assets.getTypedExtendedByIdentity(LocationExtended, selected.ri);
            center       = location?.typedModel.address;
        }

        return center;
    }
}
