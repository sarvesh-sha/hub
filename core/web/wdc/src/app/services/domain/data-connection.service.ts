import {Injectable} from "@angular/core";
import {ApiService} from "app/services/domain/api.service";
import {ConditionNode, ConditionNodeType, LogicNode} from "app/services/domain/tag-conditions";
import * as Models from "app/services/proxy/model/models";
import * as moment from "moment-timezone";

@Injectable({providedIn: "root"})
export class DataConnectionService
{
    private pointClasses: PointClass[]         = [];
    private equipmentClasses: EquipmentClass[] = [];
    private units: Unit[]                      = [];
    private kinds: Kind[]                      = [];
    private buildings: Building[]              = [];
    private equipments: Equipment[]            = [];
    private points: Point[]                    = [];

    private pointSchemas: { [id: string]: Models.TimeSeriesPropertyType } = {};

    private site: Site;

    private metadataInitialized   = false;
    private pointClassInitialized = false;

    private metadata: Promise<Models.EquipmentAggregation>;

    constructor(private apis: ApiService)
    {
    }

    private ensureMetadata(): Promise<Models.EquipmentAggregation>
    {
        if (!this.metadata)
        {
            this.metadata = this.apis.dataConnection.equipmentAggregation();
        }

        return this.metadata;
    }

    private async initializePointClasses()
    {
        if (this.pointClassInitialized)
        {
            return;
        }

        let response = await this.ensureMetadata();

        let seenUnits        = {};
        let seenKinds        = {};
        let seenPointClasses = {};


        let pointClasses     = response.pointClasses;
        let equipmentClasses = response.equipmentClasses;

        for (let pc of pointClasses)
        {
            if (pc.ignorePointIfMatched)
            {
                continue;
            }

            let unitId       = pc.unitId;
            let kindId       = pc.kindId;
            let pointClassId = pc.id;

            if (unitId && unitId !== -1 && !seenUnits[unitId])
            {
                seenUnits[unitId] = true;
                this.units.push({
                                    UnitID         : unitId,
                                    UnitDescription: pc.unitDescription
                                });
            }

            if (kindId && kindId !== -1 && !seenKinds[kindId])
            {
                seenKinds[kindId] = true;
                this.kinds.push({
                                    KindID         : kindId,
                                    KindDescription: pc.kindDescription
                                });
            }

            if (pointClassId && !seenPointClasses[pointClassId])
            {
                seenPointClasses[pointClassId] = true;
                this.pointClasses.push({
                                           PointClassID         : pointClassId,
                                           PointClassDescription: pc.pointClassDescription,
                                           PointClassName       : pc.pointClassName,
                                           PointClassType       : pc.type,
                                           KindID               : pc.kindId,
                                           UnitID               : pc.unitId
                                       });
            }
        }

        for (let ec of equipmentClasses)
        {
            this.equipmentClasses.push({
                                           EquipClassID         : ec.id,
                                           EquipClassDescription: ec.description,
                                           EquipClassName       : ec.equipClassName
                                       });
        }

        this.pointClassInitialized = true;
    }

    private async initializeFromMetadata(savedFilter: Models.FilterPreferences,
                                         unclassified: boolean,
                                         belowThresholdId: number)
    {
        if (this.metadataInitialized)
        {
            return;
        }

        try
        {
            tableau.log("Fetching global metadata.");
            let metadata = await this.ensureMetadata();
            await this.initializePointClasses();
            if (!this.site)
            {
                this.site = await this.fetchSite();
            }

            let siteId = this.site ? this.site.SiteID : undefined;

            this.processLocations(metadata.locationHierarchy, "", "");
            this.processEquipment(metadata.equipments, "");

            let controllerIds                                    = metadata.controllers;
            let schemaRequests: Models.TimeSeriesSchemaRequest[] = [];

            let filteredPoints = new Set<string>();

            if (savedFilter)
            {
                tableau.log(`Fetching saved filter.`);
                let filter = new Models.DeviceElementFilterRequest();
                this.deserializeFilter(savedFilter, filter);
                let results = await this.apis.assets.getFiltered(filter);
                for (let id of results.results)
                {
                    filteredPoints.add(id.sysId);
                }
                tableau.log(`Filtering to ${filteredPoints.size} points.`);

                let points            = await this.apis.assets.getBatch([...filteredPoints]);
                let uniqueControllers = new Set<string>(points.map((p) => p.parentAsset?.sysId)
                                                              .filter((id) => !!id));
                controllerIds         = [...uniqueControllers];
            }

            tableau.log(`Fetching metadata for ${controllerIds.length} controllers.`);
            await Promise.all(controllerIds.map(async (controllerId) =>
                                                {
                                                    let controllerAggregation = await this.apis.dataConnection.controllerMetadataAggregation(controllerId,
                                                                                                                                             unclassified || !!savedFilter,
                                                                                                                                             belowThresholdId);

                                                    for (let pointInfo of controllerAggregation.points)
                                                    {
                                                        if (!filteredPoints.size || filteredPoints.has(pointInfo.pointId))
                                                        {
                                                            this.points.push({
                                                                                 PointID         : pointInfo.pointId,
                                                                                 PointName       : pointInfo.pointName,
                                                                                 PointNameRaw    : pointInfo.pointNameRaw,
                                                                                 PointNameBackup : pointInfo.pointNameBackup,
                                                                                 PointDescription: null,
                                                                                 EquipID         : pointInfo.equipmentId,
                                                                                 PointClassID    : pointInfo.pointClassId,
                                                                                 BuildingID      : pointInfo.locationSysId,
                                                                                 SiteID          : siteId,
                                                                                 NetworkID       : controllerAggregation.networkNumber,
                                                                                 InstanceID      : controllerAggregation.instanceNumber,
                                                                                 ObjectID        : pointInfo.identifier
                                                                             });

                                                            if (pointInfo.identifier.indexOf("multi_state") == 0)
                                                            {
                                                                schemaRequests.push(Models.TimeSeriesSchemaRequest.newInstance({sysId: pointInfo.pointId}));
                                                            }
                                                        }
                                                    }
                                                }));

            tableau.log(`Fetching schemas for ${schemaRequests.length} points.`);

            while (schemaRequests.length)
            {
                let batch   = schemaRequests.splice(0, 500);
                let schemas = await this.apis.assetTimeSeries.getSchemaBatch(batch);
                for (let i = 0; i < schemas.length; i++)
                {
                    this.pointSchemas[batch[i].sysId] = schemas[i].properties["present_value"];
                }
            }

            tableau.log(`Successfully fetched metadata`);
        }
        finally
        {
            this.metadataInitialized = true;
        }
    }

    private processLocations(locations: Models.LocationHierarchy[],
                             parentId: string,
                             parentName: string)
    {
        if (!locations?.length) return;

        for (let location of locations)
        {
            let fullName = !parentName ? location.name : `${parentName} - ${location.name}`;
            this.buildings.push({
                                    BuildingID       : location.ri.sysId,
                                    ParentBuildingID : parentId,
                                    BuildingName     : fullName,
                                    BuildingShortName: location.name,
                                    Type             : location.type,
                                    SiteID           : this.site?.SiteID
                                });

            this.processLocations(location.subLocations, location.ri.sysId, fullName);
        }
    }

    private processEquipment(equipments: Models.EquipmentHierarchy[],
                             parentId: string)
    {
        if (!equipments?.length) return;

        for (let equipment of equipments)
        {
            this.equipments.push({
                                     EquipID      : equipment.sysId,
                                     ParentEquipID: parentId,
                                     EquipName    : equipment.name,
                                     EquipClassID : equipment.equipmentClassId,
                                     BuildingID   : equipment.locationSysId
                                 });

            this.processEquipment(equipment.children, equipment.sysId);
        }
    }

    protected deserializeFilter(input: Models.FilterPreferences,
                                output: Models.DeviceElementFilterRequest)
    {
        output.locationIDs = input.locationIDs;

        // Deserialize all relevant data
        output.tagsQuery = DataConnectionService.generateChildQuery(input.pointClassIDs, input.isClassified);

        output.parentTagsQuery = DataConnectionService.generateParentQuery(input.equipmentIDs, input.equipmentClassIDs);

        output.parentIDs = input.deviceIDs;

        DataConnectionService.updateFiltersFromSamplingInput(output, input.isSampling);
    }

    public static updateFiltersFromSamplingInput(filters: Models.DeviceElementFilterRequest,
                                                 sampling: Models.FilterPreferenceBoolean)
    {
        switch (sampling)
        {
            case Models.FilterPreferenceBoolean.Yes:
                filters.hasAnySampling = true;
                filters.hasNoSampling  = false;
                break;

            case Models.FilterPreferenceBoolean.No:
                filters.hasAnySampling = false;
                filters.hasNoSampling  = true;
                break;

            default:
                filters.hasAnySampling = false;
                filters.hasNoSampling  = false;
                break;
        }
    }

    public static generateChildQuery(pointClassIDs: string[],
                                     classified: Models.FilterPreferenceBoolean)
    {
        let pointClassCondition = this.generateQuery(pointClassIDs, ConditionNodeType.POINT);
        let classifiedCondition = this.generateClassifiedQuery(classified);
        if (pointClassCondition && classifiedCondition)
        {
            return Models.TagsConditionBinaryLogic.newInstance({
                                                                   op: Models.TagsConditionOperator.And,
                                                                   a : pointClassCondition,
                                                                   b : classifiedCondition
                                                               });
        }
        return pointClassCondition || classifiedCondition;
    }

    public static generateParentQuery(equipmentIDs: string[],
                                      equipmentClassIDs: string[]): Models.TagsCondition
    {
        let equipmentClassCondition = this.generateQuery(equipmentClassIDs, ConditionNodeType.EQUIPMENT);
        let equipmentCondition      = this.generateQuery(equipmentIDs, ConditionNodeType.ASSET);
        if (equipmentClassCondition && equipmentCondition)
        {
            return Models.TagsConditionBinaryLogic.newInstance({
                                                                   op: Models.TagsConditionOperator.And,
                                                                   a : equipmentCondition,
                                                                   b : equipmentClassCondition
                                                               });
        }
        return equipmentClassCondition || equipmentCondition;
    }

    public static generateQuery(classIDs: string[],
                                type: ConditionNodeType): Models.TagsCondition
    {
        let nodes: ConditionNode[] = [];

        for (let classId of classIDs || [])
        {
            let node   = new ConditionNode(type, false);
            node.value = classId;
            nodes.push(node);
        }

        switch (nodes.length)
        {
            case 0:
                return null;

            case 1:
                return nodes[0].toModel();

            default:
                let parentNode = new LogicNode(Models.TagsConditionOperator.Or);
                parentNode.children.push(...nodes);
                return parentNode.toModel();
        }
    }

    public static generateClassifiedQuery(classified: Models.FilterPreferenceBoolean): Models.TagsCondition
    {
        if (classified)
        {
            let node = new ConditionNode(ConditionNodeType.CLASSIFIED, classified === Models.FilterPreferenceBoolean.No);
            return node.toModel();
        }

        return null;
    }

    public async getPointClasses()
    {
        await this.initializePointClasses();
        return this.pointClasses;
    }

    public async getEquipmentClasses()
    {
        await this.initializePointClasses();
        return this.equipmentClasses;
    }

    public async getUnits()
    {
        await this.initializePointClasses();
        return this.units;
    }

    public async getKinds()
    {
        await this.initializePointClasses();
        return this.kinds;
    }

    public async getPoints(savedFilter: Models.FilterPreferences,
                           unclassified: boolean,
                           belowThresholdId: number)
    {
        await this.initializeFromMetadata(savedFilter, unclassified, belowThresholdId);
        return this.points;
    }

    public async getSites()
    {
        if (!this.site)
        {
            this.site = await this.fetchSite();
        }

        if (this.site)
        {
            return [
                this.site
            ];
        }

        return [];
    }

    public getPointSchema(id: string): Models.TimeSeriesPropertyType
    {
        return this.pointSchemas[id];
    }

    private async fetchSite(): Promise<Site>
    {
        let site = await this.apis.dataConnection.getSite();

        if (site)
        {
            return {
                SiteID       : site.id,
                SiteName     : site.name,
                SiteShortName: ""
            };
        }

    }

    public async getBuildings(savedFilter: Models.FilterPreferences,
                              unclassified: boolean,
                              belowThresholdId: number)
    {
        await this.initializeFromMetadata(savedFilter, unclassified, belowThresholdId);
        return this.buildings;
    }

    public async getEquipments(savedFilter: Models.FilterPreferences,
                               unclassified: boolean,
                               belowThresholdId: number)
    {
        await this.initializeFromMetadata(savedFilter, unclassified, belowThresholdId);
        return this.equipments;
    }
}

export interface Point
{
    PointID: string;
    PointDescription: string;
    PointName: string;
    PointNameRaw: string;
    PointNameBackup: string;
    BuildingID: string;
    EquipID: string;
    PointClassID: string;
    SiteID: number;
    NetworkID: number;
    InstanceID: number;
    ObjectID: string;
}

export interface TrendRecord
{
    PointID: string;
    DateTime: string;
    NumericValue: number;
    StringValue: string;
}

export interface PointClass
{
    PointClassID: number;
    PointClassDescription: string;
    PointClassName: string;
    PointClassType: Models.PointClassType;
    KindID: number;
    UnitID: number;
}

export interface EquipmentClass
{
    EquipClassID: number;
    EquipClassName: string;
    EquipClassDescription: string;
}

export interface Unit
{
    UnitID: number;
    UnitDescription: string;
}

export interface Kind
{
    KindID: number;
    KindDescription: string;
}

export interface Building
{
    BuildingID: string;
    ParentBuildingID: string;
    BuildingName: string;
    BuildingShortName: string;
    Type: string;
    SiteID: number;
}

export interface Site
{
    SiteID: number;
    SiteName: string;
    SiteShortName: string;
}

export interface Equipment
{
    EquipID: string;
    ParentEquipID: string;
    EquipClassID: string;
    EquipName: string;
    BuildingID: string;
}

export class TrendFetcher
{
    private readonly flushSize    = 1200000;
    private readonly maxBatchSize = 5000;

    private totalSamples        = 0;
    private totalPoints: number;
    private startTimeMillis: number;
    private data: TrendRecord[] = [];
    private batchSize           = 200;
    private pointsInBatch       = 0;

    constructor(private apis: ApiService,
                private dataConnection: DataConnectionService,
                private pointIds: string[],
                private connectionId: string,
                private defaultStartDate: moment.Moment,
                private timezone: string,
                private progressCallback: (trends: TrendRecord[]) => void)
    {
        this.totalPoints = pointIds.length;
    }

    public async getTrendRecords()
    {
        this.startTimeMillis = new Date().valueOf();

        tableau.log(`Fetching trends for ${this.pointIds.length} points.`);

        try
        {
            let pointPromises: Promise<void>[] = [];
            while (this.pointIds.length)
            {
                let sysId = this.pointIds.pop();
                pointPromises.push(this.processPoint(sysId));

                if (pointPromises.length === this.batchSize || this.pointIds.length === 0)
                {
                    await Promise.all(pointPromises);
                    this.flushRecords();
                    pointPromises = [];
                }
            }
        }
        catch (err)
        {
            tableau.log("Error fetching trends " + err);
        }

        this.flushRecords();
    }

    private async processPoint(id: string)
    {
        try
        {
            let startDate = await this.computeStartDate(id);

            let req        = new Models.TimeSeriesSinglePropertyRequest();
            req.spec       = new Models.TimeSeriesPropertyRequest();
            req.spec.sysId = id;
            req.spec.prop  = "present_value";

            req.rangeStart = startDate;

            let trend = await this.apis.assetTimeSeries.getValuesSingle(req);
            this.pointsInBatch++;
            let lastSampleDate = await this.processSamples(trend, id);
            if (lastSampleDate)
            {
                await this.apis.dataConnection.setLastSample(this.connectionId, id, lastSampleDate);
            }
        }
        catch (err)
        {
            tableau.log(`Error fetching sample for sysId:${id} ${err.message} ${err.stack}`);
        }
    }

    private processSamples(trend: Models.TimeSeriesSinglePropertyResponse,
                           id: string): Date
    {
        if (trend && trend.timestamps && trend.results && trend.results.values && trend.results.values.length)
        {
            let offset = this.getDateTimezoneOffsetMillis();
            let date   = new Date();

            let schema = this.dataConnection.getPointSchema(id);

            for (let j = 0; j < trend.results.values.length; j++)
            {
                let timestamp = trend.timestamps[j];
                let sample    = trend.results.values[j];
                let value: any;

                date.setTime(timestamp * 1000 + offset);

                if (trend.results.enumLookup)
                {
                    value = trend.results.enumLookup[sample];
                }
                else
                {
                    value = sample;
                }

                value = this.getNumericValue(value);

                let stringValue;

                if (schema && schema.values)
                {
                    value = parseInt(value);

                    let enumVal = schema.values.find((val) => val.value === value);
                    if (enumVal)
                    {
                        stringValue = enumVal.name;
                    }
                }

                this.data.push({
                                   DateTime    : this.formatDate(date),
                                   NumericValue: typeof value === "number" ? value : null,
                                   StringValue : stringValue || (typeof value === "string" ? value : null),
                                   PointID     : id
                               });
            }

            return date;
        }

        return null;
    }

    private getDateTimezoneOffsetMillis(): number
    {
        let date            = new Date();
        let desiredTzOffset = moment.tz.zone(this.timezone)
                                    .utcOffset(date.valueOf());
        let currentTzOffset = date.getTimezoneOffset();
        return (currentTzOffset - desiredTzOffset) * 60 * 1000;
    }

    private formatDate(d: Date): string
    {
        let minutes        = d.getMinutes();
        let minutesPadding = minutes < 10 ? "0" : "";
        let seconds        = d.getSeconds();
        let secondsPadding = seconds < 10 ? "0" : "";
        return `${d.getMonth() + 1}/${d.getDate()}/${d.getFullYear()} ${d.getHours()}:${minutesPadding + minutes}:${secondsPadding + seconds}`;
    }

    private getNumericValue(value: any): number
    {
        if (typeof value === "number")
        {
            return value;
        }
        else if (typeof value === "string")
        {
            if (value === "active")
            {
                return 1;
            }
            else if (value === "inactive")
            {
                return 0;
            }
            if (value === "NaN")
            {
                return null;
            }
        }

        return value;
    }

    private flushRecords()
    {
        let flushSize = this.data.length;
        while (this.data.length)
        {
            let chunk = this.data.splice(0, 100000);
            this.progressCallback(chunk);
        }

        this.totalSamples += flushSize;
        let nowMillis   = new Date().valueOf();
        let diff        = nowMillis - this.startTimeMillis;
        let rate        = 1000 * (this.totalSamples / diff);
        let diffSeconds = Math.floor(diff / 1000);

        let numPoints = this.totalPoints - this.pointIds.length;

        tableau.log(`Processed ${flushSize} (${this.totalSamples} total) samples from ${numPoints}/${this.totalPoints} points at ${rate} samples/sec. Elapsed time: ${diffSeconds} seconds`);

        // Try to optimize batch size so we get close to the flushSize each batch
        let newBatchSize = Math.round(this.batchSize * (this.flushSize / (flushSize || 1)));
        newBatchSize     = Math.max(1, newBatchSize);
        newBatchSize     = Math.min(this.maxBatchSize, newBatchSize);
        this.batchSize   = newBatchSize;

        tableau.log("Updating batch size to be " + this.batchSize);

        this.data = [];
    }

    private async computeStartDate(id: string): Promise<Date>
    {
        let startDate  = this.defaultStartDate ? this.defaultStartDate.toDate() : undefined;
        let lastSample = await this.apis.dataConnection.getLastSample(this.connectionId, id);
        if (lastSample)
        {
            startDate = new Date(lastSample);
            // Add 1 second to avoid fetching same sample twice
            startDate.setTime(startDate.getTime() + 1000);
        }

        return startDate;
    }
}
