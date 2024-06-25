import {Injectable, Type} from "@angular/core";

import {ReportError} from "app/app.service";
import {WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {AlertHistoryExtended, AlertsHistoryService} from "app/services/domain/alert-history.service";
import {AlertsService} from "app/services/domain/alerts.service";
import {ApiService} from "app/services/domain/api.service";
import {AssetGraphExtended, SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {AssetsService, DeviceElementExtended, LocationExtended} from "app/services/domain/assets.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import {AlertExtended, EventsService} from "app/services/domain/events.service";
import {LocationsService} from "app/services/domain/locations.service";
import {SettingsService} from "app/services/domain/settings.service";
import {EngineeringUnitsDescriptorExtended, UnitsService} from "app/services/domain/units.service";
import * as Models from "app/services/proxy/model/models";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";
import {DataSourceWizardPurpose, DataSourceWizardState} from "app/shared/charting/data-source-wizard/data-source-wizard-dialog.component";
import {ColorConfigurationExtended} from "app/shared/colors/color-configuration-extended";
import {FilterableTimeRangeExtended, RecurringWeeklyScheduleExtended} from "app/shared/forms/time-range/range-selection-extended";
import {ImagePreviewTypeMeta} from "app/shared/image/image-preview.component";

import {ErrorService} from "framework/services/error.service";
import {UtilsService} from "framework/services/utils.service";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";
import {ControlOption} from "framework/ui/control-option";
import {Future, inParallel} from "framework/utils/concurrency";

import {Subject} from "rxjs";

@Injectable()
export class WidgetDataService
{
    /**
     * Constructor
     */
    constructor(private errors: ErrorService,
                public api: ApiService,
                private alertHistory: AlertsHistoryService,
                private alerts: AlertsService,
                private events: EventsService,
                private assets: AssetsService,
                private settings: SettingsService,
                private locations: LocationsService)
    {
    }

    //--//

    /**
     * Get the feed of alerts.
     */
    @ReportError
    async getAlertFeed(locations: string[],
                       alertTypes: Models.AlertType[],
                       rangeStart?: Date,
                       rangeEnd?: Date,
                       maxNumAlerts?: number): Promise<AlertHistoryExtended[]>
    {
        // get the feed ids
        let ids = await this.api.alerts.getAlertFeed(rangeStart, rangeEnd);

        if (!isNaN(maxNumAlerts)) ids = ids.slice(0, maxNumAlerts);

        // get the full feed records
        let history = await this.alertHistory.getExtendedBatch(ids);

        // get extended feed data
        await inParallel<AlertHistoryExtended>(history, this.fetchAlertFeedExtendedData);

        // prepare our result
        let result: AlertHistoryExtended[] = [];

        // evaluate each record to check if it matches the specified filters
        for (let record of history)
        {
            let alert = await record.getAlert();

            if (locations)
            {
                let loc = await alert.getLocation();
                if (!loc) continue;

                if (!this.matchesLocation(loc.model.sysId, locations)) continue;
            }

            if (!this.matchesAlertType(alert.typedModel.type, alertTypes)) continue;

            result.push(record);
        }

        return result;
    }

    private async fetchAlertFeedExtendedData(row: AlertHistoryExtended): Promise<void>
    {
        let di = await row.getAlert();
        await di.getLocation();
    }

    private matchesLocation(test: string,
                            locations: string[]): boolean
    {
        if (locations && locations.length)
        {
            for (let location of locations)
            {
                if (test == location)
                {
                    return true;
                }
            }

            return false;
        }
        else
        {
            return true;
        }
    }

    private matchesAlertType(test: Models.AlertType,
                             alertTypes: Models.AlertType[]): boolean
    {
        if (alertTypes && alertTypes.length)
        {
            for (let alertType of alertTypes)
            {
                if (test == alertType)
                {
                    return true;
                }
            }

            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * Get some aggregated alert totals
     */
    async getAlertAggregates(filters: Models.AlertFilterRequest): Promise<AlertSummaryValues>
    {
        // get matching alerts
        if (!filters) filters = new Models.AlertFilterRequest();
        let alertIDs = await this.events.getList(filters);
        let alerts   = await this.events.getTypedExtendedBatch(AlertExtended, alertIDs.results);

        // find count of critical alerts
        let critical: number = 0;
        for (let alert of alerts)
        {
            if (alert.typedModel.severity == Models.AlertSeverity.CRITICAL)
            {
                critical++;
            }
        }

        let locationID: string = null;
        if (filters.locationIDs && filters.locationIDs.length) locationID = filters.locationIDs[0];

        return {
            id                  : locationID,
            percentChangedLast30: -20,
            total               : alerts.length,
            critical            : critical
        };
    }

    /**
     * Get alerts by location
     */
    @ReportError
    async getAlertLocations(loadLocation: (location: Models.Location) => Promise<Models.LongitudeLatitude>,
                            locations: string[],
                            alertTypes: Models.AlertType[],
                            alertSeverities: Models.AlertSeverity[] = [],
                            rollupType?: Models.LocationType): Promise<AlertLocation[]>
    {
        let filter = Models.AlertFilterRequest.newInstance({
                                                               locationIDs      : locations,
                                                               locationInclusive: true,
                                                               alertTypeIDs     : alertTypes,
                                                               alertStatusIDs   : [
                                                                   Models.AlertStatus.active,
                                                                   Models.AlertStatus.muted
                                                               ],
                                                               alertSeverityIDs : alertSeverities
                                                           });

        // get all allowed locations based on selection, includes all children/ancestors of the selection
        // if we want all locations, the locations parameter is null/empty and we continue on
        let allowedLocations = new Set<string>();
        if (locations?.length)
        {
            let hierarchy         = await this.locations.getLocationHierarchy();
            let selectedLocations = new Set<string>(locations);

            for (let topLocation of hierarchy)
            {
                collectLocations(topLocation);
            }

            function collectLocations(location: Models.LocationHierarchy): boolean
            {
                if (selectedLocations.has(location.ri.sysId))
                {
                    allowedLocations.add(location.ri.sysId);
                    collectAllChildren(location);
                    return true;
                }
                else
                {
                    let childSelected = false;
                    for (let sub of (location.subLocations || []))
                    {
                        if (collectLocations(sub))
                        {
                            childSelected = true;
                        }
                    }

                    if (childSelected) allowedLocations.add(location.ri.sysId);
                    return childSelected;
                }
            }

            function collectAllChildren(location: Models.LocationHierarchy)
            {
                for (let sub of (location?.subLocations || []))
                {
                    allowedLocations.add(sub.ri.sysId);
                    collectAllChildren(sub);
                }
            }
        }

        let alertLocations = await this.alerts.getSummary(filter, rollupType, Models.SummaryFlavor.location);

        // prepare our result
        let result: AlertLocation[] = [];

        for (let summary of alertLocations)
        {
            let location = await this.assets.getTypedExtendedById(LocationExtended, summary.id);
            // check that the location is one that was filtered, skip if it was not. If topLocations is empty, then we are including all locations
            if (!location || allowedLocations.size && !allowedLocations.has(location.model.sysId)) continue;

            let alertLocation          = new AlertLocation();
            alertLocation.id           = summary.id;
            alertLocation.count        = summary.count;
            alertLocation.locationName = summary.label;

            let typedModel = location.typedModel;
            if (typedModel.geo)
            {
                alertLocation.position = typedModel.geo;
            }
            else if (loadLocation)
            {
                try
                {
                    alertLocation.position = await loadLocation(typedModel);
                }
                catch (err)
                {
                    // At least we tried
                }
            }

            result.push(alertLocation);
        }

        return result;
    }

    /**
     * Get some aggregated totals (for widgets)
     */
    async getDeviceAggregates(filters: Models.DeviceFilterRequest): Promise<DeviceSummaryValues>
    {
        // get matching alerts
        if (!filters) filters = new Models.DeviceFilterRequest();

        let response  = await this.assets.getList(filters);
        let deviceIDs = response.results;

        // found count of missing
        // ** to do **

        let locationID: string = null;
        if (filters.locationIDs && filters.locationIDs.length) locationID = filters.locationIDs[0];

        return {
            id         : locationID,
            addedLast30: "+30",
            total      : deviceIDs.length,
            missing    : Math.ceil(deviceIDs.length * 0.01)
        };
    }

    /**
     * @param filterableRanges to pull valid ranges from - return null if filterableRanges falsy
     * @param enforceUniqueness
     */
    public getValidRanges(filterableRanges: Models.FilterableTimeRange[],
                          enforceUniqueness: boolean = false): Models.FilterableTimeRange[]
    {
        if (!filterableRanges) return null;

        let validFilterables: Models.FilterableTimeRange[] = [];
        for (let currFilterable of filterableRanges)
        {
            let currRange = currFilterable.range;
            if (!currFilterable.isFilterApplied || RecurringWeeklyScheduleExtended.scheduleIsValid(currFilterable.filter))
            {
                if (currRange.range)
                {
                    if (!enforceUniqueness || this.isUniquePreset(currFilterable, validFilterables)) validFilterables.push(currFilterable);
                }
                else if (currRange.start && currRange.end && currRange.start <= currRange.end)
                {
                    if (!enforceUniqueness || this.isUniqueCustom(currFilterable, validFilterables)) validFilterables.push(currFilterable);
                }
            }
        }
        return validFilterables;
    }

    private isUniquePreset(presetFilterable: Models.FilterableTimeRange,
                           list: Models.FilterableTimeRange[]): boolean
    {
        let presetFilterableExtended = new FilterableTimeRangeExtended(presetFilterable);
        let presetOverlaps           = list.filter((validFilterable) => validFilterable.range.range === presetFilterable.range.range);
        return presetOverlaps.every((presetOverlap) => !presetFilterableExtended.functionallyEquivalentFilter(presetOverlap));
    }

    private isUniqueCustom(customFilterable: Models.FilterableTimeRange,
                           list: Models.FilterableTimeRange[]): boolean
    {
        let customRange              = customFilterable.range;
        let customFilterableExtended = new FilterableTimeRangeExtended(customFilterable);
        let customOverlaps           = list.filter((validFilterable) =>
                                                   {
                                                       let validRange = validFilterable.range;
                                                       return !validRange.range &&
                                                              validRange.start === customRange.start &&
                                                              validRange.end === customRange.end;
                                                   });
        return customOverlaps.every((presetOverlap) =>
                                        !customFilterableExtended.functionallyEquivalentFilter(presetOverlap));
    }

    public getVisualizationModeOptions(tableOnly: boolean): ControlOption<Models.HierarchicalVisualizationType>[]
    {
        let options: ControlOption<Models.HierarchicalVisualizationType>[] = [
            new ControlOption(Models.HierarchicalVisualizationType.TABLE, "Table"),
            new ControlOption(Models.HierarchicalVisualizationType.TABLE_WITH_BAR, "Bar Table")
        ];

        if (!tableOnly)
        {
            options.push(new ControlOption(Models.HierarchicalVisualizationType.BUBBLEMAP, "Bubble Chart", undefined, tableOnly));
            options.push(new ControlOption(Models.HierarchicalVisualizationType.TREEMAP, "Tree Chart", undefined, tableOnly));
            options.push(new ControlOption(Models.HierarchicalVisualizationType.DONUT, "Donut Chart", undefined, tableOnly));
            options.push(new ControlOption(Models.HierarchicalVisualizationType.SUNBURST, "Sunburst Chart", undefined, tableOnly));
            options.push(new ControlOption(Models.HierarchicalVisualizationType.PIE, "Pie Chart", undefined, tableOnly));
            options.push(new ControlOption(Models.HierarchicalVisualizationType.PIEBURST, "Sunburst Chart - Pie", undefined, tableOnly));
        }

        return options;
    }
}

export class AlertSummaryValues
{
    id: string;
    percentChangedLast30: number;
    total: number;
    critical: number;
}

export class DeviceSummaryValues
{
    id: string;
    addedLast30: string;
    total: number;
    missing: number;
}

export class AlertLocation
{
    id: string;

    locationName: string;

    position: Models.LongitudeLatitude;

    count: number;
}

//--//

export interface WidgetTypeDefinition<T extends Models.WidgetConfiguration> extends Type<WidgetConfigurationExtended<T>>
{
}

export interface WidgetTypeConfig<S extends Models.WidgetConfiguration, T extends WidgetConfigurationExtended<S>, U extends WidgetTypeDefinition<S>>
{
    friendlyName: string;
    typeName: string;
    model: Type<S>;
    component: Type<WidgetBaseComponent<S, T>>;
    classes?: string[];
    dashboardCreatable: boolean;
    subgroupCreatable: boolean;
    maximizable: boolean;
    defaultWidth: number;
    defaultHeight: number;
    hostScalableText: boolean;
    needsProtector: boolean;
    documentation: ImagePreviewTypeMeta;
}

export class WidgetDescriptor<S extends Models.WidgetConfiguration, T extends WidgetConfigurationExtended<S>, U extends WidgetTypeDefinition<S>>
{
    definition: U;

    constructor(public readonly config: WidgetTypeConfig<S, T, U>)
    {
    }
}

export abstract class WidgetConfigurationExtended<T extends Models.WidgetConfiguration>
{
    private static s_nameToDescriptor: Map<string, WidgetDescriptor<any, any, any>> = new Map();

    private static s_modelPrototypeToDescriptor: Map<Type<any>, WidgetDescriptor<any, any, any>> = new Map();

    private static s_definitionConstructorToDescriptor: Map<WidgetTypeDefinition<any>, WidgetDescriptor<any, any, any>> = new Map();

    //--//

    public static WidgetDef<S extends Models.WidgetConfiguration, T extends WidgetConfigurationExtended<S>, U extends WidgetTypeDefinition<S>>(config: WidgetTypeConfig<S, T, U>): any
    {
        return function (definition: U)
        {
            return WidgetConfigurationExtended.registerWidget(config, definition);
        };
    }

    static registerWidget<S extends Models.WidgetConfiguration, T extends WidgetConfigurationExtended<S>, U extends WidgetTypeDefinition<S>>(config: WidgetTypeConfig<S, T, U>,
                                                                                                                                             definition: U): any
    {
        let desc = new WidgetDescriptor(config);

        if (config.model)
        {
            // Map from model's prototype to widget descriptor.
            WidgetConfigurationExtended.s_modelPrototypeToDescriptor.set(config.model.prototype, desc);
        }

        WidgetConfigurationExtended.s_nameToDescriptor.set(config.typeName, desc);

        //
        // Link definition and configuration.
        //
        WidgetConfigurationExtended.s_definitionConstructorToDescriptor.set(definition, desc);
        desc.definition = definition;

        // Link the descriptor to the widget's prototype.
        let blockDef          = definition.prototype;
        blockDef.m_descriptor = desc;

        return definition;
    }

    public static getPreviewMeta(type: string): ImagePreviewTypeMeta
    {
        // Get type meta from @WidgetDef
        return WidgetConfigurationExtended.fromName(type)
                                          .getDescriptor().config.documentation;
    }

    public getDescriptor(): WidgetDescriptor<any, any, any>
    {
        let prototype = Object.getPrototypeOf(this);
        return prototype.m_descriptor;
    }

    public static getDescriptorForDefinition(definition: WidgetTypeDefinition<any>): WidgetDescriptor<any, any, any>
    {
        return WidgetConfigurationExtended.s_definitionConstructorToDescriptor.get(definition);
    }

    public static getWidgetName(definition: WidgetTypeDefinition<any>): string
    {
        let desc = WidgetConfigurationExtended.getDescriptorForDefinition(definition);
        if (desc)
        {
            return desc.config.typeName;
        }

        return null;
    }

    public static enumerateDescriptors(): WidgetTypeConfig<Models.WidgetConfiguration, WidgetConfigurationExtended<Models.WidgetConfiguration>, WidgetTypeDefinition<Models.WidgetConfiguration>>[]
    {
        return UtilsService.mapIterable(WidgetConfigurationExtended.s_definitionConstructorToDescriptor.values(), (value) => value.config);
    }

    public static enumerateNames(): string[]
    {
        return UtilsService.mapIterable(WidgetConfigurationExtended.s_definitionConstructorToDescriptor.values(), (value) => value.config.typeName);
    }

    public static fromName(name: string): WidgetConfigurationExtended<any>
    {
        let desc = WidgetConfigurationExtended.s_nameToDescriptor.get(name);
        if (desc)
        {
            let ext: WidgetConfigurationExtended<any> = new desc.definition();
            ext.model                                 = ext.newModel();
            return ext;
        }

        throw Error(`Can't find prototype for widget '${name}'`);
    }

    public static fromConfigModel<T extends Models.WidgetConfiguration>(model: T): WidgetConfigurationExtended<T>
    {
        let modelPrototype = Object.getPrototypeOf(model);
        let desc           = WidgetConfigurationExtended.s_modelPrototypeToDescriptor.get(modelPrototype);
        let ext            = WidgetConfigurationExtended.fromDescriptor(desc, model);
        if (ext) return ext;

        throw Error(`Can't find prototype for model ${modelPrototype}`);
    }

    private static fromDescriptor<T extends Models.WidgetConfiguration>(desc: WidgetDescriptor<T, WidgetConfigurationExtended<T>, WidgetTypeDefinition<T>>,
                                                                        model: T): WidgetConfigurationExtended<T>
    {
        if (!desc || !model) return null;

        let ext: WidgetConfigurationExtended<T> = new desc.definition();
        ext.model                               = model;
        return ext;
    }

    static copyModel(source: Models.WidgetConfiguration,
                     target: Models.WidgetConfiguration)
    {
        target.id              = source.id;
        // omit size because deprecated
        target.name            = source.name;
        target.description     = source.description;
        target.locations       = source.locations;
        target.toolbarBehavior = source.toolbarBehavior;
    }

    private m_descriptor: WidgetDescriptor<any, any, any>;

    public model: T;

    protected newModel(): T
    {
        let desc = this.getDescriptor();
        if (desc && desc.config.model)
        {
            return new desc.config.model();
        }

        throw new Error("Unable to determine widget type.");
    }

    public startingStep(): string
    {
        return null;
    }

    public initializeForWizard()
    {
        this.model = this.newModel();
        this.initializeForWizardInner();
    }

    protected abstract initializeForWizardInner(): void;

    public abstract getBindings(): Models.AssetGraphBinding[];
}

export const WidgetDef = WidgetConfigurationExtended.WidgetDef;

//--//

export class ControlPointsGroupExtended
{
    initialized: Future<void>;

    model: Models.ControlPointsGroup;
    desc: EngineeringUnitsDescriptorExtended;

    private m_defaultColorConfig: Models.ColorConfiguration;

    get colorConfig(): Models.ColorConfiguration
    {
        return this.model.colorConfig || this.m_defaultColorConfig;
    }

    set colorConfig(colorConfig: Models.ColorConfiguration)
    {
        if (colorConfig)
        {
            this.m_defaultColorConfig = null;
            this.model.colorConfig    = colorConfig;
        }
    }

    private m_graphsHost: GraphConfigurationHost;
    private m_sharedGraphs: Models.SharedAssetGraph[];

    get graphsHost(): GraphConfigurationHost
    {
        if (!this.m_graphsHost) this.updateGraphsHost(false);
        return this.m_graphsHost;
    }

    private m_dataSourceState: DataSourceWizardState;

    private m_numControlPoints: number;
    get numControlPoints(): number
    {
        if (this.m_numControlPoints) return this.m_numControlPoints;
        if (this.model.selections) return this.model.selections.identities.length;
        return 0;
    }

    set numControlPoints(num: number)
    {
        this.m_numControlPoints = num;
    }

    get settingsValid(): boolean
    {
        if (!this.model.aggregationType) return false;
        if (!this.model.groupAggregationType) return false;
        return true;
    }

    get settingsTooltip(): string
    {
        if (!this.model.aggregationType) return "Select control points' aggregation type";
        if (!this.model.groupAggregationType) return "Select group's aggregation type";

        return "Group settings";
    }

    constructor(private appDomain: AppDomainContext,
                model?: Models.ControlPointsGroup)
    {
        this.initialized = new Future<void>();
        this.model       = model ?? new Models.ControlPointsGroup();
        if (!this.model.limitMode) this.model.limitMode = Models.AggregationLimit.None;
        if (!this.model.selections) this.model.selections = Models.ControlPointsSelection.newInstance({identities: []});

        this.initializeDescriptor();
    }

    public static newModel(base: Partial<Models.ControlPointsGroup>): Models.ControlPointsGroup
    {
        return Models.ControlPointsGroup.newInstance({
                                                         aggregationType     : Models.AggregationTypeId.MAX,
                                                         groupAggregationType: Models.AggregationTypeId.MAX,
                                                         valuePrecision      : 2,
                                                         ...(base || {})
                                                     });
    }

    public async clone(): Promise<ControlPointsGroupExtended>
    {
        let clone = new ControlPointsGroupExtended(this.appDomain, this.model);
        await clone.initialized;
        return clone;
    }

    public getDataSourceState(purpose: DataSourceWizardPurpose,
                              externalGraphsHost: GraphConfigurationHost,
                              force: boolean): DataSourceWizardState
    {
        if (force || !this.m_dataSourceState) this.resetDataSourceState(purpose, externalGraphsHost);

        return this.m_dataSourceState;
    }

    public resetDataSourceState(purpose: DataSourceWizardPurpose,
                                externalGraphsHost: GraphConfigurationHost)
    {
        this.m_graphsHost = null;

        let isNew              = this.model.graph || this.model.pointInput ? !this.model.pointInput?.nodeId : !this.model.selections.identities?.length;
        this.m_dataSourceState = new DataSourceWizardState(isNew, purpose, null, externalGraphsHost, false);
        this.m_dataSourceState.updateForControlPointsGroup(this.model);
        this.updateGraphsHost(false);
        this.m_dataSourceState.graphsHost = this.graphsHost;
    }

    public ensureColorConfig(mode: Models.HierarchicalVisualizationType,
                             otherColors: string[])
    {
        if (this.model.colorConfig) return;
        this.m_defaultColorConfig = ControlPointsGroupExtended.ensureColorConfig(this.model, mode, otherColors);
    }

    private async initializeDescriptor()
    {
        if (this.model.unitsFactors) this.desc = await this.appDomain.units.resolveDescriptor(this.model.unitsFactors, false);

        this.initialized.resolve();
    }

    public async updateFactors()
    {
        if (this.desc)
        {
            let unitsResolved = await this.appDomain.units.resolveDescriptor(this.desc.rawFactors ?? this.desc.model.factors, false);
            let newUnits      = EngineeringUnitsDescriptorExtended.extractFactors(unitsResolved);
            if (this.model.unitsFactors && UnitsService.areEquivalent(this.model.unitsFactors, newUnits))
            {
                await ColorConfigurationExtended.convertUnits(this.appDomain.units, this.model.colorConfig, this.model.unitsFactors, newUnits);
            }

            this.model.unitsFactors = newUnits;
        }
    }

    public async updateUnitsDisplay(units: UnitsService)
    {
        this.model.unitsDisplay = await units.getDimensionlessFlavor(EngineeringUnitsDescriptorExtended.extractFactors(this.desc));
    }

    public updateGraphsHost(force: boolean)
    {
        if (!this.m_dataSourceState) return;

        if (!this.m_graphsHost || force)
        {
            if (!this.m_dataSourceState.localGraph) this.m_dataSourceState.localGraph = AssetGraphExtended.emptyModel();

            this.m_sharedGraphs = [SharedAssetGraphExtended.newModel(Models.AssetGraph.deepClone(this.m_dataSourceState.localGraph), SharedAssetGraphExtended.LOCAL_GRAPH_ID, "Asset Graph")];

            this.m_graphsHost = {
                hostContext  : "Control Point Group",
                graphsChanged: new Subject<void>(),
                getGraphs    : () => this.m_sharedGraphs,
                resolveGraphs: () => SharedAssetGraphExtended.loadGraphs(this.appDomain, this.m_sharedGraphs),
                canRemove    : () => false,
                canRemoveNode: () => true
            };
        }
    }

    public async bindToElement(element: DeviceElementExtended)
    {
        let propSchema = await element.getSchemaProperty(DeviceElementExtended.PRESENT_VALUE);
        this.desc      = await this.appDomain.units.resolveDescriptor(propSchema?.unitsFactors, false);
        await this.updateFactors();
    }

    //--//

    public static ensureColorConfig(group: Models.ControlPointsGroup,
                                    mode: Models.HierarchicalVisualizationType,
                                    otherColors: string[]): Models.ColorConfiguration
    {
        if (group?.colorConfig)
        {
            if (group.colorConfig.segments?.length) return group.colorConfig;

            group.colorConfig = null;
        }

        switch (mode)
        {
            case Models.HierarchicalVisualizationType.TABLE:
            case Models.HierarchicalVisualizationType.TABLE_WITH_BAR:
                return ColorConfigurationExtended.defaultWidgetModel();

            default:
                let color = ChartColorUtilities.nextBestColor(otherColors, null, true);
                return Models.ColorConfiguration.newInstance({
                                                                 segments: [
                                                                     Models.ColorSegment.newInstance({
                                                                                                         color    : color,
                                                                                                         stopPoint: Models.ColorStopPoint.MIN
                                                                                                     }),
                                                                     Models.ColorSegment.newInstance({
                                                                                                         color    : color,
                                                                                                         stopPoint: Models.ColorStopPoint.MAX
                                                                                                     })
                                                                 ]
                                                             });
        }
    }

    public static getLocalGraphs(groups: Models.ControlPointsGroup[]): Models.SharedAssetGraph[]
    {
        let sharedGraphs = [];
        for (let group of groups)
        {
            if (group.graph)
            {
                let equivalentGraph = sharedGraphs.find((sharedGraph) => UtilsService.compareJson(sharedGraph.graph, group.graph));
                if (!equivalentGraph)
                {
                    sharedGraphs.push(SharedAssetGraphExtended.newModel(group.graph, null, null));
                }
            }
        }

        return sharedGraphs;
    }

    public static isValid(group: Models.ControlPointsGroup,
                          allowNoGroupAggregation: boolean,
                          allowNoSources: boolean,
                          enforceNaming: boolean,
                          enforceGroupAggregationType: boolean): boolean
    {
        if (enforceNaming && !group.name) return false;
        if (!group.selections) return false;
        if (group.unitsFactors == null) return false;
        if (!group.aggregationType) return false;
        if (!ControlPointsGroupExtended.isValidGroupAggregationType(group.groupAggregationType, allowNoGroupAggregation)) return false;
        if (!allowNoSources && group.selections.identities.length == 0 && !group.graph) return false;
        if (group.graph && !group.pointInput) return false;
        if (enforceGroupAggregationType && !group.groupAggregationType) return false;

        return true;
    }

    public static isValidGroupAggregationType(aggregationType: Models.AggregationTypeId,
                                              allowNoGroupAggregation: boolean): boolean
    {
        switch (aggregationType)
        {
            case Models.AggregationTypeId.MIN:
            case Models.AggregationTypeId.MAX:
            case Models.AggregationTypeId.MEAN:
            case Models.AggregationTypeId.SUM:
                return true;

            case Models.AggregationTypeId.NONE:
                return allowNoGroupAggregation;

            default:
                return false;

        }
    }
}
