import {Injectable, Type} from "@angular/core";

import {AppContext, ReportError} from "app/app.service";
import {ControlPointMetadata, TimeSeriesSourceParameters} from "app/customer/visualization/time-series-utils";
import {AlertHistoryExtended} from "app/services/domain/alert-history.service";
import {ApiService} from "app/services/domain/api.service";
import {AssetGraphExtended} from "app/services/domain/asset-graph.service";
import {BackgroundActivityExtended} from "app/services/domain/background-activities.service";
import * as SharedSvc from "app/services/domain/base.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import {EnumsService} from "app/services/domain/enums.service";
import {AlertColorStrategy, AlertMarker, AlertShapeStrategy, AlertSizeStrategy} from "app/services/domain/events.service";
import {GatewayProberOperationExtended} from "app/services/domain/gateway-prober-operations.service";
import * as Models from "app/services/proxy/model/models";
import {TimeDurationExtended} from "app/shared/forms/time-range/time-duration-extended";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {Lookup, UtilsService} from "framework/services/utils.service";
import {ChartLineType} from "framework/ui/charting/core/basics";
import {ChartPoint, ChartPointProvider, ChartPointRange, ChartPointSource, ChartPointSubscriber, ChartPointType, ChartValueFormatter, DataSourceBitSet, DataSourceBoolean, DataSourceDecimal, DataSourceEnumerated, DataSourceEnumeratedSet, DataSourceInteger, DataSourceTuple, ScatterPlotPropertyTuple} from "framework/ui/charting/core/data-sources";
import {ContextMenuItemComponent} from "framework/ui/context-menu/context-menu.component";
import {ControlOption} from "framework/ui/control-option";
import {DatatableManager} from "framework/ui/datatables/datatable-manager";
import {ILazyTreeNode} from "framework/ui/dropdowns/filterable-tree.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {Memoizer, ResetMemoizers} from "framework/utils/memoizers";

import moment from "framework/utils/moment";

@Injectable()
export class AssetsService extends SharedSvc.BaseService<Models.Asset, AssetExtended>
{
    private batcherForLastValue: SharedSvc.AutoBatcher<Models.TimeSeriesLastValueRequest, Models.TimeSeriesLastValueResponse>;
    private batcherForSchema: SharedSvc.AutoBatcher<Models.TimeSeriesSchemaRequest, Models.TimeSeriesSchemaResponse>;
    private batcherForRanges: SharedSvc.AutoBatcher<Models.TimeSeriesRangeRequest, Models.TimeSeriesRangeResponse>;
    private batcherForValues: SharedSvc.AutoBatcher<Models.TimeSeriesSinglePropertyRequest, Models.TimeSeriesSinglePropertyResponse>;
    private batcherForRelationships: SharedSvc.AutoBatcher<Models.AssetRelationshipRequest, Models.AssetRelationshipResponse>;
    private batcherForActiveWorkflows: SharedSvc.AutoBatcher<string, Models.RecordIdentity[]>;

    private cachedTagsSummary: Models.TagsSummary;
    private pendingTagsSummary: Promise<Models.TagsSummary>;

    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService,
                private enums: EnumsService)
    {
        super(api, errors, cache, Models.Asset, AssetExtended.newInstance);

        this.batcherForSchema = new SharedSvc.AutoBatcher(25, 30, (batch) => SharedSvc.AutoRetryRequest(3, () =>
        {
            return this.domain.apis.assetTimeSeries.getSchemaBatch(batch);
        }));

        this.batcherForLastValue = new SharedSvc.AutoBatcher(25, 30, (batch) => SharedSvc.AutoRetryRequest(3, () =>
        {
            return this.domain.apis.assetTimeSeries.getLastValueBatch(batch);
        }));

        this.batcherForRanges = new SharedSvc.AutoBatcher(50, 50, (batch) => SharedSvc.AutoRetryRequest(3, () =>
        {
            return this.domain.apis.assetTimeSeries.getRangeBatch(batch);
        }));

        this.batcherForValues = new SharedSvc.AutoBatcher(25, 50, (batch) => SharedSvc.AutoRetryRequest(3, () =>
        {
            return this.domain.apis.assetTimeSeries.getValuesSingleBatch(batch);
        }));

        this.batcherForRelationships = new SharedSvc.AutoBatcher(25, 50, (batch) => SharedSvc.AutoRetryRequest(3, () =>
        {
            return this.domain.apis.assetRelationships.lookupBatch(batch);
        }));

        this.batcherForActiveWorkflows = new SharedSvc.AutoBatcher(25, 50, (batch) => SharedSvc.AutoRetryRequest(3, () =>
        {
            return this.domain.apis.assets.getActiveWorkflowsBatch(batch);
        }));
    }

    /**
     * Get the summary of Assets.
     */
    @ReportError
    async getSummary(filters: Models.AssetFilterRequest,
                     groupBy?: Models.SummaryFlavor): Promise<Models.SummaryResult[]>
    {
        let result = await this.api.assets.getSummary(groupBy, filters);

        // Wait and sort the results, to make it nicer for the caller.
        result.sort((a,
                     b) => UtilsService.compareStrings(a.label, b.label, true));

        return result;
    }

    public getTopLevelEquipments(): Promise<Models.RecordIdentity[]>
    {
        return this.domain.apis.assetRelationships.getTopEquipments();
    }

    public async getEquipmentTree(): Promise<ILazyTreeNode<string>[]>
    {
        let summary = new Map<string, ILazyTreeNode<string>>();

        let structure = new AssetGraphExtended(this.domain);
        structure.addNode(Models.AssetGraphNode.newInstance({
                                                                id       : "equip",
                                                                condition: new Models.TagsConditionIsEquipment()
                                                            }));

        structure.addNode(Models.AssetGraphNode.newInstance({
                                                                id       : "equip2",
                                                                optional : true,
                                                                condition: new Models.TagsConditionIsEquipment()
                                                            }));
        structure.addRelationshipTransform("equip", "equip2", Models.AssetRelationship.controls);

        let relationships = await structure.resolve();

        let filters = Models.AssetFilterRequest.newInstance({
                                                                tagsQuery: new Models.TagsConditionIsEquipment(),
                                                                sortBy   : [
                                                                    Models.SortCriteria.newInstance({
                                                                                                        column   : "name",
                                                                                                        ascending: true
                                                                                                    })
                                                                ]
                                                            });

        let response      = await this.getList(filters);
        let allEquipments = response.results;

        let parentBinding = Models.AssetGraphBinding.newInstance({nodeId: "equip"});
        let childBinding  = Models.AssetGraphBinding.newInstance({nodeId: "equip2"});
        let topLevel      = new Set<string>();

        // Collect all nodes
        for (let equipId of allEquipments)
        {
            let node: ILazyTreeNode<string> = {
                id      : equipId.sysId,
                children: []
            };
            topLevel.add(equipId.sysId);
            summary.set(equipId.sysId, node);
        }

        // Establish relationships
        for (let resp of relationships.responses)
        {
            let [parent] = resp.resolveInputIdentities(parentBinding);
            let children = resp.resolveInputIdentities(childBinding);

            for (let child of children)
            {
                summary.get(parent.sysId)
                       .children
                       .push(summary.get(child.sysId));
                topLevel.delete(child.sysId);
            }
        }

        return allEquipments.filter((eq) => topLevel.has(eq.sysId))
                            .map((eq) => summary.get(eq.sysId));
    }

    public async getEquipmentSummaries(nodes: ILazyTreeNode<string>[]): Promise<EquipmentSummary[]>
    {
        let locationMap = new Map<string, string[]>();
        let resultMap   = new Map<string, EquipmentSummary>();
        let batch       = await this.getExtendedBatch(nodes.map((node) => AssetExtended.newIdentityRaw(node.id)));

        for (let i = 0; i < nodes.length; i++)
        {
            let node  = nodes[i];
            let equip = batch[i];
            resultMap.set(equip.model.sysId, {
                id              : node.id,
                children        : node.children,
                name            : equip.model.name,
                locationName    : "",
                equipmentClassId: equip.model.equipmentClassId
            });

            let locId = equip.model.location?.sysId;
            if (!locationMap.has(locId))
            {
                locationMap.set(locId, []);
            }
            locationMap.get(locId)
                       .push(equip.model.sysId);
        }

        let locationIds = UtilsService.mapIterable(locationMap.keys(), (id) => LocationExtended.newIdentity(id));
        let locations   = await this.domain.locations.getExtended(locationIds);

        for (let loc of locations)
        {
            let equips = locationMap.get(loc.model.sysId);
            for (let equipId of equips)
            {
                resultMap.get(equipId).locationName = loc.model.name;
            }
        }

        return nodes.map((n) => resultMap.get(n.id));
    }

    /**
     * Get the list of devices, applying optional filters.
     */
    @ReportError
    public getList(filters: Models.AssetFilterRequest): Promise<Models.AssetFilterResponse>
    {
        return this.api.assets.getFiltered(filters);
    }

    /**
     * Get the list of devices, applying optional filters.
     */
    @ReportError
    public async getListWithPaging(filters: Models.AssetFilterRequest): Promise<Models.RecordIdentity[]>
    {
        let result: Models.RecordIdentity[] = [];
        let response                        = await this.getList(filters);
        result.push(...response.results);
        while (response.nextOffset)
        {
            filters.startOffset = response.nextOffset;
            response            = await this.getList(filters);
            result.push(...response.results);
        }

        return result;
    }

    public static isControlPoint(ri: Models.RecordIdentity)
    {
        switch (ri?.table)
        {
            case Models.DeviceElement.RECORD_IDENTITY:
            case Models.MetricsDeviceElement.RECORD_IDENTITY:
                return true;
        }

        return false;
    }

    /**
     * True if any filters are actually applied.
     */
    static hasAppliedFilters(filters: Models.AssetFilterRequest,
                             checkParent: boolean    = false,
                             checkTagsQuery: boolean = true): boolean
    {
        if (filters)
        {
            if (filters.discoveryRangeEnd) return true;
            if (filters.discoveryRangeStart) return true;
            if (filters.locationIDs && filters.locationIDs.length) return true;

            if (filters instanceof Models.DeviceFilterRequest)
            {
                if (filters.likeDeviceManufacturerName) return true;
                if (filters.likeDeviceProductName) return true;
                if (filters.likeDeviceModelName) return true;
            }

            if (filters instanceof Models.DeviceElementFilterRequest)
            {
                if (filters.likeFilter) return true;
                if (filters.hasAnySampling) return true;
                if (filters.hasNoSampling) return true;
            }

            if (checkTagsQuery && filters.tagsQuery instanceof Models.TagsCondition) return true;
            if (filters.parentTagsQuery instanceof Models.TagsCondition) return true;
            if (checkParent && filters.parentIDs?.length) return true;
        }

        return false;
    }

    /**
     * Return the count of records when the filters are applied.
     * @param filters
     */
    @ReportError
    async getCount(filters: Models.AssetFilterRequest): Promise<number>
    {
        return await this.api.assets.getFilteredCount(filters);
    }

    @ReportError
    async getTagsSummary(): Promise<Models.TagsSummary>
    {
        if (this.pendingTagsSummary)
        {
            return this.pendingTagsSummary;
        }

        try
        {
            this.pendingTagsSummary = this.getTagsSummaryInner();

            return await this.pendingTagsSummary;
        }
        finally
        {
            this.pendingTagsSummary = null;
        }
    }

    private async getTagsSummaryInner(): Promise<Models.TagsSummary>
    {
        let tagsSummary = await this.api.assets.tagsSummary(this.cachedTagsSummary?.version);

        if (this.cachedTagsSummary?.version != tagsSummary.version)
        {
            this.cachedTagsSummary = tagsSummary;
        }

        return this.cachedTagsSummary;
    }

    //--//

    protected cachePrefix(): string { return Models.Asset.RECORD_IDENTITY; }

    protected getRaw(id: string): Promise<Models.Asset>
    {
        return this.api.assets.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.Asset[]>
    {
        return this.api.assets.getBatch(ids);
    }

    //--//

    describeWellKnownEquipmentClasses(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("WellKnownEquipmentClass", false);
    }

    @Memoizer
    async getWellKnownEquipmentClasses(): Promise<ControlOption<Models.WellKnownEquipmentClass>[]>
    {
        let types = await this.describeWellKnownEquipmentClasses();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    describeWellKnownPointClasses(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("WellKnownPointClass", false);
    }

    @Memoizer
    async getWellKnownPointClasses(): Promise<ControlOption<Models.WellKnownPointClass>[]>
    {
        let types = await this.describeWellKnownPointClasses();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    //--//

    describeOperationalStates(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("AssetState", false);
    }

    @Memoizer
    async getOperationalStates(): Promise<ControlOption<Models.AssetState>[]>
    {
        let types = await this.describeOperationalStates();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    //--//

    describeRelationships(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("AssetRelationship", false);
    }

    @Memoizer
    async getRelationships(): Promise<ControlOption<Models.AssetRelationship>[]>
    {
        let types = await this.describeRelationships();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    //--//

    public tagsQuery(query: Models.TagsJoinQuery): Promise<string[][]>
    {
        return this.domain.apis.assets.tagsQuery(query);
    }

    async loadTable(sysIds: string[][]): Promise<Models.Asset[][]>
    {
        // Collect unique sysIds.
        let set = new Set<string>();
        for (let row of sysIds)
        {
            for (let column of row)
            {
                set.add(column);
            }
        }

        let uniqueSysIds = Array.from(set);
        let ids          = uniqueSysIds.map((id) => AssetExtended.newIdentityRaw(id));

        let assetsReturned = await this.getBatch(ids);

        let sysIdToAsset: Lookup<Models.Asset> = {};

        for (let model of assetsReturned)
        {
            if (model)
            {
                sysIdToAsset[model.sysId] = model;
            }
        }

        let results = [];
        for (let row of sysIds)
        {
            let rowResult = [];
            for (let column of row)
            {
                let asset = sysIdToAsset[column];
                if (!asset)
                {
                    // Skip row altogether if any asset is missing.
                    rowResult = null;
                    break;
                }

                rowResult.push(asset);
            }

            if (rowResult)
            {
                results.push(rowResult);
            }
        }

        return results;
    }

    public computeAggregation(request: Models.AggregationRequest): Promise<Models.AggregationResponse>
    {
        return SharedSvc.AutoRetryRequest(3, () => this.domain.apis.assetTimeSeries.aggregation(request));
    }

    async getInterpolatedValues(properties: Models.TimeSeriesPropertyRequest[],
                                rangeStart?: moment.Moment,
                                rangeEnd?: moment.Moment,
                                maxSamples?: number,
                                maxGapBetweenSamples?: number): Promise<Models.TimeSeriesMultiPropertyResponse>
    {
        let req   = new Models.TimeSeriesMultiPropertyRequest();
        req.specs = properties;

        req.maxSamples           = maxSamples;
        req.maxGapBetweenSamples = maxGapBetweenSamples;
        req.rangeStart           = rangeStart ? rangeStart.toDate() : null;
        req.rangeEnd             = rangeEnd ? rangeEnd.toDate() : null;

        return SharedSvc.AutoRetryRequest(3, () => this.domain.apis.assetTimeSeries.getValuesMultiple(req));
    }

    getSchema(sysId: string): Promise<Models.TimeSeriesSchemaResponse>
    {
        let req   = new Models.TimeSeriesSchemaRequest();
        req.sysId = sysId;

        return this.batcherForSchema.queue(req);
    }

    getLastValue(spec: Models.TimeSeriesPropertyRequest): Promise<Models.TimeSeriesLastValueResponse>
    {
        let req  = new Models.TimeSeriesLastValueRequest();
        req.spec = spec;

        return this.batcherForLastValue.queue(req);
    }

    getRange(spec: Models.TimeSeriesPropertyRequest,
             rangeStart?: moment.Moment,
             rangeEnd?: moment.Moment): Promise<Models.TimeSeriesRangeResponse>
    {
        let req  = new Models.TimeSeriesRangeRequest();
        req.spec = spec;

        if (rangeStart) req.rangeStart = rangeStart.toDate();
        if (rangeEnd) req.rangeEnd = rangeEnd.toDate();

        return this.batcherForRanges.queue(req);
    }

    async getValues(spec: Models.TimeSeriesPropertyRequest,
                    rangeStart?: moment.Moment,
                    rangeEnd?: moment.Moment,
                    maxSamples?: number,
                    maxGapBetweenSamples?: number): Promise<Models.TimeSeriesSinglePropertyResponse>
    {
        let req                  = new Models.TimeSeriesSinglePropertyRequest();
        req.spec                 = spec;
        req.maxSamples           = maxSamples;
        req.maxGapBetweenSamples = maxGapBetweenSamples;
        req.rangeStart           = rangeStart ? rangeStart.toDate() : null;
        req.rangeEnd             = rangeEnd ? rangeEnd.toDate() : null;
        req.deltaEncode          = true;

        let res = await this.batcherForValues.queue(req);
        if (res?.timestamps && res?.results?.values)
        {
            if (res.deltaEncoded)
            {
                AssetsService.deltaDecode(res.timestamps);
                AssetsService.deltaDecode(res.results.values);
            }

            return res;
        }

        return null;
    }

    static deltaDecode(values: number[])
    {
        let previousValue = NaN;

        for (let i = 0; i < values.length; i++)
        {
            let delta = values[i];
            let currentValue: number;

            if (!isNaN(previousValue) && !isNaN(delta))
            {
                currentValue = previousValue + delta;
            }
            else
            {
                currentValue = delta;
            }

            values[i] = currentValue;

            previousValue = currentValue;
        }
    }

    async lookupRelationships(assetId: string,
                              relation: Models.AssetRelationship,
                              fromParentToChildren: boolean): Promise<Models.RecordIdentity[]>
    {
        let req                  = new Models.AssetRelationshipRequest();
        req.assetId              = assetId;
        req.relationship         = relation;
        req.fromParentToChildren = fromParentToChildren;

        let res = await this.batcherForRelationships.queue(req);
        return res?.assets;
    }

    async lookupActiveWorkflows(assetId: string): Promise<Models.RecordIdentity[]>
    {
        return this.batcherForActiveWorkflows.queue(assetId);
    }

    //--//

    wrapTypedModel<E extends AssetExtended, M extends Models.Asset>(type: Type<E>,
                                                                    model: M): E
    {
        let ext = super.wrapModel(model);
        return this.asType(type, ext);
    }

    async getTypedExtendedByIdentity<E extends AssetExtended>(type: Type<E>,
                                                              id: Models.RecordIdentity): Promise<E>
    {
        let ext = await super.getExtendedByIdentity(id);
        return this.asType(type, ext);
    }

    async getTypedExtendedById<E extends AssetExtended>(type: Type<E>,
                                                        id: string): Promise<E>
    {
        let ext = await super.getExtendedById(id);
        return this.asType(type, ext);
    }

    async getTypedExtendedBatch<E extends AssetExtended>(type: Type<E>,
                                                         ids: Models.RecordIdentity[]): Promise<E[]>
    {
        let exts = await super.getExtendedBatch(ids);
        return this.typedFilter(type, exts, true);
    }

    async getTypedExtendedAll<E extends AssetExtended, F extends Models.AssetFilterRequest>(type: Type<E>,
                                                                                            filters: F): Promise<E[]>
    {
        let ids  = await this.getList(filters);
        let exts = await this.getExtendedBatch(ids.results);
        return this.typedFilter(type, exts, true);
    }

    async getTypedPage<E extends AssetExtended>(type: Type<E>,
                                                list: Models.RecordIdentity[],
                                                offset: number,
                                                limit: number): Promise<E[]>
    {
        let exts = await super.getPage(list, offset, limit);
        return this.typedFilter(type, exts, false);
    }

    async getTypedPageFromTable<E extends AssetExtended>(type: Type<E>,
                                                         table: DatatableManager<Models.RecordIdentity, AssetExtended, any>,
                                                         offset: number,
                                                         limit: number): Promise<E[]>
    {
        let exts = await super.getPageFromTable(table, offset, limit);
        return this.typedFilter(type, exts, false);
    }

    //--//

    /**
     * Create or update the asset.
     */
    @ReportError
    async save<T extends Models.Asset>(model: T): Promise<T>
    {
        if (model.sysId)
        {
            this.flushModel(model);

            await this.api.assets.update(model.sysId, undefined, model);
            return <Promise<T>>this.get(model.sysId, model.updatedOn);
        }
        else
        {
            return <Promise<T>>this.api.assets.create(model);
        }
    }

    async canSave<T extends Models.Asset>(model: T): Promise<boolean>
    {
        if (!model.sysId)
        {
            return false;
        }

        let result = await this.domain.apis.assets.update(model.sysId, true, model);
        return !result || !result.entries || result.entries.length > 0;
    }
}

export class AssetExtended extends SharedSvc.ExtendedModel<Models.Asset>
{
    static newInstance(svc: AssetsService,
                       model: Models.Asset): AssetExtended
    {
        if (model instanceof Models.BACnetDevice)
        {
            return BACnetDeviceExtended.newInstance(svc, model);
        }

        if (model instanceof Models.IpnDevice)
        {
            return IpnDeviceExtended.newInstance(svc, model);
        }

        if (model instanceof Models.Device)
        {
            return DeviceExtended.newInstance(svc, model);
        }

        if (model instanceof Models.HostAsset)
        {
            return HostExtended.newInstance(svc, model);
        }

        if (model instanceof Models.GatewayAsset)
        {
            return GatewayExtended.newInstance(svc, model);
        }

        if (model instanceof Models.NetworkAsset)
        {
            return NetworkExtended.newInstance(svc, model);
        }

        if (model instanceof Models.MetricsDeviceElement)
        {
            return MetricsDeviceElementExtended.newInstance(svc, model);
        }

        if (model instanceof Models.DeviceElement)
        {
            return DeviceElementExtended.newInstance(svc, model);
        }

        if (model instanceof Models.LogicalAsset)
        {
            return LogicalAssetExtended.newInstance(svc, model);
        }

        if (model instanceof Models.Location)
        {
            return LocationExtended.newInstance(svc, model);
        }

        return new AssetExtended(svc, model, Models.Asset.RECORD_IDENTITY);
    }

    static newIdentityRaw(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.Asset.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get typedModel(): Models.Asset
    {
        return this.model;
    }

    async getAll()
    {
        let location = await this.getLocation();
        if (location)
        {
            await location.getRecursiveName();
        }
    }

    //--//

    public walkParentRelations(callback: (ext: AssetExtended) => boolean,
                               ...relations: Models.AssetRelationship[]): Promise<AssetExtended>
    {
        return this.walkParentRelationsInner(new Set<string>(), callback, ...relations);
    }

    private async walkParentRelationsInner(seen: Set<string>,
                                           callback: (ext: AssetExtended) => boolean,
                                           ...relations: Models.AssetRelationship[]): Promise<AssetExtended>
    {
        for (let relation of relations)
        {
            for (let parent of await this.getExtendedParentsOfRelation(relation))
            {
                if (!seen.has(parent.model.sysId))
                {
                    seen.add(parent.model.sysId);

                    if (callback(parent))
                    {
                        return parent;
                    }

                    let res = await parent.walkParentRelationsInner(seen, callback, ...relations);
                    if (res != null)
                    {
                        return res;
                    }
                }
            }
        }

        return null;
    }

    public async getParentsOfRelation(relation: Models.AssetRelationship): Promise<Models.RecordIdentity[]>
    {
        return this.domain.assets.lookupRelationships(this.model.sysId, relation, false);
    }

    public async getExtendedParentsOfRelation(relation: Models.AssetRelationship): Promise<AssetExtended[]>
    {
        let parentIds = await this.getParentsOfRelation(relation);
        return this.domain.assets.getExtendedBatch(parentIds);
    }

    public async getChildrenOfRelation(relation: Models.AssetRelationship): Promise<Models.RecordIdentity[]>
    {
        return this.domain.assets.lookupRelationships(this.model.sysId, relation, true);
    }

    public async getExtendedChildrenOfRelation(relation: Models.AssetRelationship): Promise<AssetExtended[]>
    {
        let childIds = await this.getChildrenOfRelation(relation);
        return this.domain.assets.getExtendedBatch(childIds);
    }

    //--//

    async getChildrenCounts(): Promise<{ numChildEquipment: number, numChildControlPoints: number }>
    {
        let ret      = {
            numChildEquipment    : 0,
            numChildControlPoints: 0
        };
        let children = await this.getChildrenOfRelation(Models.AssetRelationship.controls);
        for (let child of children)
        {
            if (AssetsService.isControlPoint(child))
            {
                ret.numChildControlPoints++;
            }
            else
            {
                ret.numChildEquipment++;
            }
        }

        return ret;
    }

    //--//

    @Memoizer
    public getActiveWorkflows(): Promise<Models.RecordIdentity[]>
    {
        return this.domain.assets.lookupActiveWorkflows(this.model.sysId);
    }

    @Memoizer
    public getParent(): Promise<AssetExtended>
    {
        return this.domain.assets.getExtendedByIdentity(this.model.parentAsset);
    }

    @Memoizer
    public async getEquipment(): Promise<AssetExtended>
    {
        let equipment = await this.getParentsOfRelation(Models.AssetRelationship.controls);
        if (equipment?.length)
        {
            return this.domain.assets.getTypedExtendedByIdentity(AssetExtended, equipment[0]);
        }

        return null;
    }

    @Memoizer
    public getLocation(): Promise<LocationExtended>
    {
        return this.domain.assets.getTypedExtendedByIdentity(LocationExtended, this.model.location);
    }

    @ResetMemoizers
    public setLocation(locSysId: string)
    {
        if (locSysId)
        {
            if (!this.model.location)
            {
                this.model.location = LocationExtended.newIdentity(locSysId);
            }
            else
            {
                this.model.location.sysId = locSysId;
            }
        }
        else
        {
            this.model.location = null;
        }
    }

    getTravelLog(rangeStart?: moment.Moment,
                 rangeEnd?: moment.Moment,
                 maxGapForSegment?: number,
                 maxDurationPerSegment?: number): Promise<Models.AssetTravelLog>
    {
        return this.domain.apis.assets.getTravelLog(this.model.sysId, maxGapForSegment, maxDurationPerSegment, rangeStart ? rangeStart.toDate() : null, rangeEnd ? rangeEnd.toDate() : null);
    }

    //--//

    async checkUpdate(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.assets.update(this.model.sysId, true, this.model);
        return result && result.entries ? result.entries : [];
    }

    async checkRemove(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.assets.remove(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        await this.flush();
        return await this.domain.apis.assets.remove(this.model.sysId);
    }

    public refreshDisplayName()
    {
        this.model.displayName = this.model.name;
    }

    async canSave(): Promise<boolean>
    {
        let validations = this.validate();

        if (validations.length > 0)
        {
            return false;
        }

        return this.domain.assets.canSave(this.model);
    }

    async save(): Promise<AssetExtended>
    {
        let validations = this.validate();

        if (!validations.length)
        {
            // save entity
            this.model = await this.domain.assets.save(this.model);

            return this;
        }
        else
        {
            this.domain.assets.errors.error("VALIDATION_ERRORS",
                                            "Asset could not be saved.",
                                            validations.map(
                                                a => a.message));
            throw new Error("VALIDATION_ERRORS");
        }
    }

    //--//

    public async getAlertHistory(rangeStart?: moment.Moment,
                                 rangeEnd?: moment.Moment): Promise<AlertHistoryExtended[]>
    {
        let startDate = rangeStart ? rangeStart.toDate() : undefined;
        let endDate   = rangeEnd ? rangeEnd.toDate() : undefined;

        let historyIds = await this.domain.apis.assets.getHistoryByID(this.model.sysId, startDate, endDate);
        return this.domain.alertsHistory.getExtendedBatch(historyIds);
    }

    //--//

    @ResetMemoizers
    public setTag(tag: string,
                  values?: string[]): Promise<string[]>
    {
        return this.domain.apis.assets.setTag(this.model.sysId, tag, values);
    }

    @ResetMemoizers
    public removeTag(tag: string): Promise<boolean>
    {
        return this.domain.apis.assets.removeTag(this.model.sysId, tag);
    }
}

export type AssetChangeSubscription = SharedSvc.DbChangeSubscription<Models.Asset>;

export class LocationExtended extends AssetExtended
{
    static newInstance(svc: AssetsService,
                       model: Models.Location): LocationExtended
    {
        return new LocationExtended(svc, model, Models.Location.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.Location.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    recursiveName: string;

    get typedModel(): Models.Location
    {
        return <Models.Location>this.model;
    }

    async save(): Promise<LocationExtended>
    {
        return <LocationExtended>await super.save();
    }

    navigateTo()
    {
        let appContext = this.injectService(AppContext);

        appContext.ui.navigation.go("/configuration/locations/location", [this.model.sysId]);
    }

    @Memoizer
    public async getRecursivePath(): Promise<string>
    {
        let cur: LocationExtended = this;
        let recursiveName: string = null;
        while (true)
        {
            cur = await cur.getParent();
            if (!cur)
            {
                break;
            }

            if (recursiveName)
            {
                recursiveName = cur.model.name + " - " + recursiveName;
            }
            else
            {
                recursiveName = cur.model.name;
            }
        }

        return recursiveName;
    }

    @Memoizer
    public async getRecursiveName(): Promise<string>
    {
        let cur: LocationExtended = this;
        let recursiveName: string = null;
        while (true)
        {
            if (recursiveName)
            {
                recursiveName = cur.model.name + " - " + recursiveName;
            }
            else
            {
                recursiveName = cur.model.name;
            }

            cur = await cur.getParent();
            if (!cur)
            {
                this.recursiveName = recursiveName;
                break;
            }
        }

        return this.recursiveName;
    }

    public getParent(): Promise<LocationExtended>
    {
        return <Promise<LocationExtended>>super.getParent();
    }

    @Memoizer
    async getInner(): Promise<LocationExtended[]>
    {
        let filters       = new Models.AssetFilterRequest();
        filters.parentIDs = [this.model.sysId];
        filters.sortBy    = [
            Models.SortCriteria.newInstance(
                {
                    column   : "name",
                    ascending: true
                })
        ];

        return this.domain.assets.getTypedExtendedAll(LocationExtended, filters);
    }

    @Memoizer
    async getInnerDeep(inclusive: boolean = true): Promise<LocationExtended[]>
    {
        return this.domain.locations.getLocationsUnder(this, inclusive);
    }

    @Memoizer
    public async getEmailOptions(): Promise<Models.DeliveryOptions>
    {
        return this.domain.apis.locations.getEmailOptions(this.model.sysId);
    }

    @ResetMemoizers
    public async setEmailOptions(options: Models.DeliveryOptions): Promise<Models.DeliveryOptions>
    {
        return this.domain.apis.locations.setEmailOptions(this.model.sysId, options);
    }

    @Memoizer
    public async getSmsOptions(): Promise<Models.DeliveryOptions>
    {
        return this.domain.apis.locations.getSmsOptions(this.model.sysId);
    }

    @ResetMemoizers
    public async setSmsOptions(options: Models.DeliveryOptions): Promise<Models.DeliveryOptions>
    {
        return this.domain.apis.locations.setSmsOptions(this.model.sysId, options);
    }
}

export class LogicalAssetExtended extends AssetExtended
{
    static newInstance(svc: AssetsService,
                       model: Models.LogicalAsset): LogicalAssetExtended
    {
        return new LogicalAssetExtended(svc, model, Models.LogicalAsset.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.LogicalAsset.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get typedModel(): Models.LogicalAsset
    {
        return <Models.LogicalAsset>this.model;
    }

    async save(): Promise<LogicalAssetExtended>
    {
        return <LogicalAssetExtended>await super.save();
    }
}

export class DeviceExtended extends AssetExtended
{
    static newInstance(svc: AssetsService,
                       model: Models.Device): DeviceExtended
    {
        return new DeviceExtended(svc, model, Models.Device.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.Device.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get typedModel(): Models.Device
    {
        return <Models.Device>this.model;
    }

    @Memoizer
    public getIdentityDescriptor(): string
    {
        let desc = this.typedModel.identityDescriptor;
        if (desc instanceof Models.BACnetDeviceDescriptor && desc.address)
        {
            return `${desc.address.networkNumber}/${desc.address.instanceNumber}`;
        }

        if (desc instanceof Models.IpnDeviceDescriptor)
        {
            return desc.name;
        }

        return "";
    }

    @Memoizer
    public getTransportDescriptor(): string
    {
        let desc = this.typedModel.identityDescriptor;
        if (desc instanceof Models.BACnetDeviceDescriptor && desc.transport instanceof Models.UdpTransportAddress)
        {
            return `${desc.transport.host}:${desc.transport.port}`;
        }

        return "";
    }

    validateInner(errors: SharedSvc.ModelValidationError[])
    {
        super.validateInner(errors);

        let model = this.typedModel;

        // Commenting this, we want to allow assets without Location.
        //
        // if (!model.location || !model.location.sysId)
        // {
        //     SharedSvc.ModelValidationError.add(errors, "location", "A Location must be selected.");
        // }
    }

    async startRediscovery(forceListObjects?: boolean,
                           forceReadObjects?: boolean): Promise<BackgroundActivityExtended>
    {
        let id = await this.domain.apis.devices.runRediscovery(this.model.sysId, forceListObjects, forceReadObjects);
        return this.domain.backgroundActivities.getExtendedByIdentity(id);
    }

    async save(): Promise<DeviceExtended>
    {
        return <DeviceExtended>await super.save();
    }
}

export class BACnetDeviceExtended extends DeviceExtended
{
    static newInstance(svc: AssetsService,
                       model: Models.BACnetDevice): BACnetDeviceExtended
    {
        return new BACnetDeviceExtended(svc, model, Models.BACnetDevice.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.BACnetDevice.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get typedModel(): Models.BACnetDevice
    {
        return <Models.BACnetDevice>this.model;
    }

    async save(): Promise<BACnetDeviceExtended>
    {
        return <BACnetDeviceExtended>await super.save();
    }
}

export class IpnDeviceExtended extends DeviceExtended
{
    static newInstance(svc: AssetsService,
                       model: Models.IpnDevice): IpnDeviceExtended
    {
        return new IpnDeviceExtended(svc, model, Models.IpnDevice.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.IpnDevice.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get typedModel(): Models.IpnDevice
    {
        return <Models.IpnDevice>this.model;
    }

    async save(): Promise<IpnDeviceExtended>
    {
        return <IpnDeviceExtended>await super.save();
    }
}

export class NetworkExtended extends AssetExtended
{
    static newInstance(svc: AssetsService,
                       model: Models.NetworkAsset): NetworkExtended
    {
        return new NetworkExtended(svc, model, Models.NetworkAsset.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.NetworkAsset.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    public static async listAll(domain: AppDomainContext,
                                sort?: boolean): Promise<NetworkExtended[]>
    {
        let networks = await domain.assets.getTypedExtendedAll(NetworkExtended, new Models.NetworkFilterRequest());

        if (sort)
        {
            networks.sort((a,
                           b) => UtilsService.compareStrings(a.model.name, b.model.name, true));
        }

        return networks;
    }

    get typedModel(): Models.NetworkAsset
    {
        return <Models.NetworkAsset>this.model;
    }

    getProtocolConfig<T extends Models.ProtocolConfig>(type: Type<T>): T
    {
        for (let cfg of this.typedModel.protocolsConfiguration || [])
        {
            if (cfg instanceof type)
            {
                return <T>cfg;
            }
        }

        return null;
    }

    @Memoizer
    public async getBoundGateway(): Promise<GatewayExtended>
    {
        let gatewayId = await this.domain.apis.discovery.getReverseBindings(this.model.sysId);
        return this.domain.assets.getTypedExtendedByIdentity(GatewayExtended, gatewayId);
    }

    validateInner(errors: SharedSvc.ModelValidationError[])
    {
        super.validateInner(errors);

        let model = this.typedModel;

        if (!model.displayName && !model.logicalName && !model.physicalName)
        {
            SharedSvc.ModelValidationError.add(errors, "name", "A name must be selected.");
        }

        if (!model.cidr)
        {
            SharedSvc.ModelValidationError.add(errors, "cidr", "A Subnet Mask must be selected.");
        }

        if (!model.location || !model.location.sysId)
        {
            SharedSvc.ModelValidationError.add(errors, "location", "A Location must be selected.");
        }
    }

    async save(): Promise<NetworkExtended>
    {
        return <NetworkExtended>await super.save();
    }

    public async autoConfig(): Promise<boolean>
    {
        return this.domain.apis.discovery.autoConfig(this.model.sysId);
    }

    updateSampling(startWithClassId: boolean,
                   stopWithoutClassId: boolean,
                   triggerConfiguration: boolean): Promise<Models.RecordIdentity>
    {
        return this.domain.apis.discovery.updateSampling(this.model.sysId, false, startWithClassId, stopWithoutClassId, triggerConfiguration);
    }

    //--//

    filterLog(filters: Models.LogEntryFilterRequest): Promise<Models.LogRange[]>
    {
        return this.domain.apis.networks.filterLog(this.model.sysId, filters);
    }

    getLog(fromOffset?: number,
           toOffset?: number,
           limit?: number): Promise<Models.LogLine[]>
    {
        return this.domain.apis.networks.getLog(this.model.sysId, fromOffset, toOffset, limit);
    }

    deleteLog(olderThan?: number): Promise<number>
    {
        return this.domain.apis.networks.deleteLog(this.model.sysId, olderThan);
    }
}

export class HostExtended extends AssetExtended
{
    static newInstance(svc: AssetsService,
                       model: Models.HostAsset): HostExtended
    {
        return new HostExtended(svc, model, Models.HostAsset.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.HostAsset.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get typedModel(): Models.HostAsset
    {
        return <Models.HostAsset>this.model;
    }

    async save(): Promise<HostExtended>
    {
        return <HostExtended>await super.save();
    }

    //--//

    filterLog(filters: Models.LogEntryFilterRequest): Promise<Models.LogRange[]>
    {
        return this.domain.apis.hosts.filterLog(this.model.sysId, filters);
    }

    getLog(fromOffset?: number,
           toOffset?: number,
           limit?: number): Promise<Models.LogLine[]>
    {
        return this.domain.apis.hosts.getLog(this.model.sysId, fromOffset, toOffset, limit);
    }

    deleteLog(olderThan?: number): Promise<number>
    {
        return this.domain.apis.hosts.deleteLog(this.model.sysId, olderThan);
    }
}

export class GatewayExtended extends AssetExtended
{
    static newInstance(svc: AssetsService,
                       model: Models.GatewayAsset): GatewayExtended
    {
        return new GatewayExtended(svc, model, Models.GatewayAsset.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.GatewayAsset.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    public static async listAll(domain: AppDomainContext,
                                sort?: boolean): Promise<GatewayExtended[]>
    {
        let gateways = await domain.assets.getTypedExtendedAll(GatewayExtended, new Models.GatewayFilterRequest());

        if (sort)
        {
            gateways.sort((a,
                           b) =>
                          {
                              let aName = a.model.name || a.typedModel.instanceId;
                              let bName = b.model.name || b.typedModel.instanceId;
                              return UtilsService.compareStrings(aName, bName, true);
                          });
        }

        return gateways;
    }

    get typedModel(): Models.GatewayAsset
    {
        return <Models.GatewayAsset>this.model;
    }

    async save(): Promise<GatewayExtended>
    {
        return <GatewayExtended>await super.save();
    }

    public async flushEntities(): Promise<BackgroundActivityExtended>
    {
        let taskId = await this.domain.apis.discovery.flushEntities(this.model.sysId);
        return this.domain.backgroundActivities.getExtendedByIdentity(taskId);
    }

    public async flushHeartbeat(): Promise<BackgroundActivityExtended>
    {
        let taskId = await this.domain.apis.discovery.flushHeartbeat(this.model.sysId);
        return this.domain.backgroundActivities.getExtendedByIdentity(taskId);
    }

    @Memoizer
    public async getBoundNetworks(): Promise<NetworkExtended[]>
    {
        let networkIds = await this.domain.apis.discovery.getBindings(this.model.sysId);
        let networks   = await this.domain.assets.getTypedExtendedBatch(NetworkExtended, networkIds);

        networks.sort((a,
                       b) => UtilsService.compareStrings(a.model.name, b.model.name, true));

        return networks;
    }

    @ResetMemoizers
    public async bindNetwork(network: NetworkExtended,
                             forceDiscovery?: boolean,
                             forceListObjects?: boolean,
                             forceReadObjects?: boolean): Promise<boolean>
    {
        await network.resetAllCachedValues();
        await this.resetAllCachedValues();

        return this.domain.apis.discovery.bind(this.model.sysId, network.model.sysId, forceDiscovery, forceListObjects, forceReadObjects);
    }

    @ResetMemoizers
    public async unbindNetwork(network: NetworkExtended): Promise<boolean>
    {
        await network.resetAllCachedValues();
        await this.resetAllCachedValues();

        return this.domain.apis.discovery.unbind(this.model.sysId, network.model.sysId);
    }

    filterLog(filters: Models.LogEntryFilterRequest): Promise<Models.LogRange[]>
    {
        return this.domain.apis.gateways.filterLog(this.model.sysId, filters);
    }

    getLog(fromOffset?: number,
           toOffset?: number,
           limit?: number): Promise<Models.LogLine[]>
    {
        return this.domain.apis.gateways.getLog(this.model.sysId, fromOffset, toOffset, limit);
    }

    deleteLog(olderThan?: number): Promise<number>
    {
        return this.domain.apis.gateways.deleteLog(this.model.sysId, olderThan);
    }

    //--//

    async checkNetworkStatus(): Promise<Models.ProberNetworkStatus>
    {
        try
        {
            return await this.domain.apis.gateways.checkNetworkStatus(this.model.sysId);
        }
        catch (e)
        {
            return null;
        }
    }

    public async startOperation(input: Models.ProberOperation): Promise<GatewayProberOperationExtended>
    {
        let op = await this.domain.apis.gateways.startOperation(this.model.sysId, input);
        return this.domain.gatewayProberOperations.wrapModel(op);
    }

    //--//

    static getUrlForDownload(domain: AppDomainContext,
                             fileName: string): string
    {
        return domain.apis.gateways.getAllLogs__generateUrl(fileName);
    }
}

export class DeviceElementExtended extends AssetExtended
{
    public static readonly PRESENT_VALUE = "present_value";

    static newInstance(svc: AssetsService,
                       model: Models.DeviceElement): DeviceElementExtended
    {
        return new DeviceElementExtended(svc, model, Models.DeviceElement.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.DeviceElement.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get isCoordinate(): boolean
    {
        let identifier = this.typedModel.identifier;
        return identifier === "latitude" || identifier === "longitude";
    }

    get id(): string
    {
        return this.model.sysId;
    }

    get typedModel(): Models.DeviceElement
    {
        return <Models.DeviceElement>this.model;
    }

    get hasSamplingSettings(): boolean
    {
        let model = this.typedModel;

        return model.samplingSettings && model.samplingSettings.length > 0;
    }

    get description(): string
    {
        let contents = this.typedModel.contents;
        return contents?.description ?? "";
    }

    validateInner(errors: SharedSvc.ModelValidationError[])
    {
        super.validateInner(errors);

        let model = this.typedModel;

        if (!model.identifier)
        {
            SharedSvc.ModelValidationError.add(errors, "identifier", "An identifier must be selected.");
        }
    }

    //--//

    @Memoizer
    public async fetchSchema(): Promise<DeviceElementSchema>
    {
        let res = await this.domain.assets.getSchema(this.model.sysId);
        return res?.properties || {};
    }

    async getSchemaProperty(property: string): Promise<Models.TimeSeriesPropertyType>
    {
        let schema = await this.fetchSchema();
        return schema[property];
    }

    getLastValue(property: string,
                 convertTo?: Models.EngineeringUnitsFactors): Promise<Models.TimeSeriesLastValueResponse>
    {
        return this.domain.assets.getLastValue(this.describeTimeSeries(property, undefined, convertTo));
    }

    async getEnumValueDisplay(value: any,
                              dimension: string = DeviceElementExtended.PRESENT_VALUE): Promise<string>
    {
        let schema = await this.getSchemaProperty(dimension);
        if (schema?.values)
        {
            if (value === false) value = 0;
            if (value === true) value = 1;

            let enVal = schema.values.find((val) => val.value === value);
            return enVal ? enVal.name : value;
        }

        return null;
    }

    getRange(property: string,
             rangeStart?: moment.Moment,
             rangeEnd?: moment.Moment,
             timeOffset?: Models.TimeDuration,
             convertTo?: Models.EngineeringUnitsFactors): Promise<Models.TimeSeriesRangeResponse>
    {
        return this.domain.assets.getRange(this.describeTimeSeries(property, timeOffset, convertTo), rangeStart, rangeEnd);
    }

    getValues(property: string,
              rangeStart: moment.Moment,
              rangeEnd: moment.Moment,
              timeOffset?: Models.TimeDuration,
              overlapInSeconds?: number,
              lastTimestamp?: moment.Moment,
              maxSamples?: number,
              maxGapBetweenSamples?: number,
              convertTo?: Models.EngineeringUnitsFactors): Promise<Models.TimeSeriesSinglePropertyResponse>
    {
        if (overlapInSeconds)
        {
            if (rangeStart) rangeStart = MomentHelper.subtract(rangeStart, overlapInSeconds, "seconds");
            if (rangeEnd) rangeEnd = MomentHelper.add(rangeEnd, overlapInSeconds, "seconds");
        }

        if (lastTimestamp)
        {
            rangeStart = lastTimestamp;
        }

        return this.domain.assets.getValues(this.describeTimeSeries(property, timeOffset, convertTo), rangeStart, rangeEnd, maxSamples, maxGapBetweenSamples);
    }

    trimValues(maxDays: number): Promise<boolean>
    {
        return this.domain.apis.assetTimeSeries.trimSamples(this.model.sysId, maxDays);
    }

    describeTimeSeries(property: string,
                       timeOffset?: Models.TimeDuration,
                       convertTo?: Models.EngineeringUnitsFactors): Models.TimeSeriesPropertyRequest
    {
        let spec             = new Models.TimeSeriesPropertyRequest();
        spec.sysId           = this.model.sysId;
        spec.prop            = property;
        spec.convertTo       = convertTo;
        spec.offsetInSeconds = TimeDurationExtended.getTimeOffset(timeOffset);
        return spec;
    }

    //--//

    async save(): Promise<DeviceElementExtended>
    {
        return <DeviceElementExtended>await super.save();
    }

    async getDataSourceForProperty(comp: BaseApplicationComponent,
                                   parameters: TimeSeriesSourceParameters): Promise<ChartPointSource<any>>
    {
        let schema = await this.getSchemaProperty(parameters.property);
        if (!schema) return null;

        let app            = comp.app;
        let enumDescriptor = await this.domain.units.getDescriptorFromFactors(parameters.units);

        if (!enumDescriptor)
        {
            enumDescriptor = await this.domain.units.resolveDescriptor(schema.unitsFactors, false);
        }

        const abbreviation = enumDescriptor ? enumDescriptor.model.displayName : "";

        let meta = await ControlPointMetadata.fromId(app, this.model.sysId);
        if (!meta) return null;

        let name = this.model.name || "";
        if (parameters.timeOffset?.amount) name += " " + TimeDurationExtended.getTimeOffsetString(parameters.timeOffset);

        let description = await meta.standardDescription();

        const tooltip = (point: ChartPoint<any>,
                         abbreviation: string) =>
        {
            let physicalName = meta.physicalName;
            if (parameters.onlyShowMovingAverage)
            {
                physicalName += " (moving average)";
            }

            const processedTimestamp = point.getProcessedTimestamp();
            return ChartPointSource.generateTooltipEntry("Name", name) +
                   ChartPointSource.generateTooltipEntry("Description", description) +
                   ChartPointSource.generateTooltipEntry("Timestamp", processedTimestamp) +
                   ChartPointSource.generateTooltipEntry("Raw", physicalName) +
                   ChartPointSource.generateTooltipEntry("Backup", meta.logicalName) +
                   ChartPointSource.valueTooltipText(point, abbreviation);
        };

        const tooltipText = (point: ChartPoint<any>,
                             abbreviation: string) =>
        {
            const processedTimestamp = point.getProcessedTimestamp();
            return ChartPointSource.generateTooltipEntryText("Name", name) +
                   ChartPointSource.generateTooltipEntryText("Description", description) +
                   ChartPointSource.generateTooltipEntryText("Timestamp", processedTimestamp) +
                   ChartPointSource.generateTooltipEntryText("Raw", meta.physicalName) +
                   ChartPointSource.generateTooltipEntryText("Backup", meta.logicalName) +
                   ChartPointSource.valueTooltipText(point, abbreviation);
        };

        let source: ChartPointSource<any>;

        switch (schema.type)
        {
            case Models.TimeSeriesSampleType.Integer:
            {
                if (schema.isBoolean)
                {
                    let formatter: ChartValueFormatter<boolean> = {
                        getTooltip(point: ChartPoint<boolean>): string
                        {
                            return tooltip(point, "");
                        },
                        getTooltipText(point: ChartPoint<boolean>): string
                        {
                            return tooltipText(point, "");
                        }
                    };

                    source          = new DataSourceBoolean(app, formatter);
                    source.lineType = ChartLineType.StepLeft;
                }
                else
                {
                    let formatter: ChartValueFormatter<number> = {
                        getTooltip(point: ChartPoint<number>): string
                        {
                            return tooltip(point, abbreviation);
                        },
                        getTooltipText(point: ChartPoint<number>): string
                        {
                            return tooltipText(point, abbreviation);
                        }
                    };

                    source = new DataSourceInteger(app, formatter);

                    let desc = await app.domain.units.resolveDescriptor(schema.unitsFactors, false);
                    if (desc)
                    {
                        switch (desc.model.family)
                        {
                            case Models.EngineeringUnitsFamily.Binary:
                            case Models.EngineeringUnitsFamily.Enumerated:
                                source.lineType = ChartLineType.StepLeft;
                                break;
                        }
                    }
                }
                break;
            }

            case Models.TimeSeriesSampleType.Decimal:
            {
                let formatter: ChartValueFormatter<number> = {
                    getTooltip(point: ChartPoint<number>): string
                    {
                        return tooltip(point, abbreviation);
                    },
                    getTooltipText(point: ChartPoint<number>): string
                    {
                        return tooltipText(point, abbreviation);
                    }
                };

                source = new DataSourceDecimal(app, formatter);
                break;
            }

            case Models.TimeSeriesSampleType.Enumerated:
            {
                let formatter: ChartValueFormatter<number | string> = {
                    getTooltip(point: ChartPoint<number | string>): string
                    {
                        return tooltip(point, "");
                    },
                    getTooltipText(point: ChartPoint<number | string>): string
                    {
                        return tooltipText(point, "");
                    }
                };

                source          = new DataSourceEnumerated(app, formatter, schema.values);
                source.lineType = ChartLineType.StepLeft;
                break;
            }

            case Models.TimeSeriesSampleType.BitSet:
            {
                let formatter: ChartValueFormatter<string[]> = {
                    getTooltip(point: ChartPoint<string[]>): string
                    {
                        return tooltip(point, "");
                    },
                    getTooltipText(point: ChartPoint<string[]>): string
                    {
                        return tooltipText(point, "");
                    }
                };

                source = new DataSourceBitSet(app, formatter);
                break;
            }

            case Models.TimeSeriesSampleType.EnumeratedSet:
            {
                let formatter: ChartValueFormatter<string[]> = {
                    getTooltip(point: ChartPoint<string[]>): string
                    {
                        return tooltip(point, "");
                    },
                    getTooltipText(point: ChartPoint<string[]>): string
                    {
                        return tooltipText(point, "");
                    }
                };

                source          = new DataSourceEnumeratedSet(app, formatter);
                source.lineType = ChartLineType.StepLeft;
                break;
            }

            default:
                return null;
        }

        let samplingSettings = this.typedModel.samplingSettings || [];
        let sampling         = samplingSettings.find((val) => val.propertyName == parameters.property);
        if (sampling)
        {
            source.expectedSamplingRate = sampling.samplingPeriod;
        }

        source.label         = parameters.property;
        source.noValueMarker = schema.noValueMarker;
        source.provider      = new ChartPointProviderImpl(comp,
                                                          this,
                                                          parameters.property,
                                                          source,
                                                          parameters.includeAlerts,
                                                          parameters.rangeStart,
                                                          parameters.rangeEnd,
                                                          parameters.timeOffset,
                                                          parameters.units);

        return source;
    }

    public async setDesiredState(state: Lookup<any>)
    {
        await this.domain.apis.deviceElements.setDesiredState(this.model.sysId, state);
    }
}

export class MetricsDeviceElementExtended extends DeviceElementExtended
{
    static newInstance(svc: AssetsService,
                       model: Models.MetricsDeviceElement): MetricsDeviceElementExtended
    {
        return new MetricsDeviceElementExtended(svc, model, Models.MetricsDeviceElement.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.MetricsDeviceElement.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get typedModel(): Models.MetricsDeviceElement
    {
        return <Models.MetricsDeviceElement>this.model;
    }

    @Memoizer
    public getMetricsDefinition(): Promise<Models.MetricsDefinition>
    {
        return this.domain.apis.assets.lookupMetrics(this.model.sysId);
    }

    async save(): Promise<MetricsDeviceElementExtended>
    {
        return <MetricsDeviceElementExtended>await super.save();
    }

    getValues(property: string,
              rangeStart: moment.Moment,
              rangeEnd: moment.Moment,
              timeOffset?: Models.TimeDuration,
              overlapInSeconds?: number,
              lastTimestamp?: moment.Moment,
              maxSamples?: number,
              maxGapBetweenSamples?: number,
              convertTo?: Models.EngineeringUnitsFactors): Promise<Models.TimeSeriesSinglePropertyResponse>
    {
        // For Metrics, we need to fetch all the data.
        overlapInSeconds = undefined;
        lastTimestamp    = undefined;
        maxSamples       = undefined;

        return super.getValues(property, rangeStart, rangeEnd, timeOffset, overlapInSeconds, lastTimestamp, maxSamples, maxGapBetweenSamples, convertTo);
    }
}

//--//

class ChartPointProviderSource
{
    public ready: boolean = false;
    public request: Models.TimeSeriesPropertyRequest;

    private dbSubscription: SharedSvc.DbChangeSubscription<Models.Asset>;

    constructor(private comp: SharedSvc.BaseComponentWithTables,
                private deviceElemExt: DeviceElementExtended,
                property: string,
                units: Models.EngineeringUnitsFactors)
    {
        this.request = Models.TimeSeriesPropertyRequest.newInstance(
            {
                sysId    : deviceElemExt.model.sysId,
                prop     : property,
                convertTo: units
            });
    }

    subscribe(innerSubscriptionFn: () => Promise<void>)
    {
        let subscriptionFn  = () =>
        {
            this.ready = true;
            return innerSubscriptionFn();
        };
        this.dbSubscription = this.comp.subscribe(this.deviceElemExt, subscriptionFn);
    }

    unsubscribe(): boolean
    {
        if (this.dbSubscription)
        {
            this.comp.removeSubscription(this.dbSubscription);
            return true;
        }

        return false;
    }
}

export class ChartPointTupleProviderImpl extends ChartPointProvider
{
    private providerSources: ScatterPlotPropertyTuple<ChartPointProviderSource>;

    private m_subscriber: ChartPointSubscriber;

    private m_fetching: boolean = false;

    private m_lastTimestamp: moment.Moment;
    private m_previousTimestamp: number = undefined;
    private m_previousDelta: number     = 0;
    private m_previousValues: ScatterPlotPropertyTuple<number>;

    private m_pointRange: ChartPointRange<ScatterPlotPropertyTuple<number>> = null;

    private get sourcesReady(): boolean
    {
        if (!this.providerSources.valueX.ready) return false;
        if (!this.providerSources.valueY.ready) return false;
        return !(this.providerSources.valueZ && !this.providerSources.valueZ.ready);
    }

    constructor(private comp: SharedSvc.BaseComponentWithTables,
                private elements: ScatterPlotPropertyTuple<DeviceElementExtended>,
                private property: string,
                private assets: AssetsService,
                public readonly source: DataSourceTuple,
                rangeStart: moment.Moment,
                rangeEnd: moment.Moment,
                public readonly timeOffset: Models.TimeDuration,
                private maxGapForNewRange: number = 30 * 60,
                private units: ScatterPlotPropertyTuple<Models.EngineeringUnitsFactors>)
    {
        super(rangeStart, rangeEnd, source);

        if ((this.units.valueZ != null) !== (this.elements.valueZ != null)) this.units = undefined;
    }

    public computeMax(rangeStart: moment.Moment,
                      rangeEnd: moment.Moment): Promise<number>
    {
        return undefined;
    }

    public computeMin(rangeStart: moment.Moment,
                      rangeEnd: moment.Moment): Promise<number>
    {
        return undefined;
    }

    public computeSum(rangeStart: moment.Moment,
                      rangeEnd: moment.Moment): Promise<number>
    {
        return undefined;
    }

    public startStreamingSamples(maxSamplesPerBatch: number,
                                 subscriber: ChartPointSubscriber)
    {
        if (!this.providerSources)
        {
            this.m_subscriber = subscriber;

            let sources = [
                new ChartPointProviderSource(this.comp, this.elements.valueX, this.property, this.units.valueX),
                new ChartPointProviderSource(this.comp, this.elements.valueY, this.property, this.units.valueY)
            ];
            if (this.elements.valueZ) sources.push(new ChartPointProviderSource(this.comp, this.elements.valueZ, this.property, this.units.valueZ));

            for (let source of sources)
            {
                source.subscribe(() =>
                                 {
                                     if (this.sourcesReady) return this.fetchSamples();
                                     return null;
                                 });
            }

            this.providerSources = new ScatterPlotPropertyTuple(0, sources[0], sources[1], sources[2]);

            this.fetchSamples();
        }
    }

    public stopStreamingSamples()
    {
        if (this.m_subscriber)
        {
            this.transitionToInactive();

            this.m_subscriber = null;
        }

        this.providerSources.valueX.unsubscribe();
        this.providerSources.valueY.unsubscribe();
        if (this.providerSources.valueZ) this.providerSources.valueZ.unsubscribe();

        this.providerSources = null;
    }

    public accessContext<T>(type: Type<T>): T
    {
        if (this.elements instanceof type) return this.elements;

        return null;
    }

    public async prepareContextMenu(point: ChartPoint<any>,
                                    root: ContextMenuItemComponent): Promise<boolean>
    {
        // Not supported for now.
        return false;
    }

    async fetchSamples()
    {
        if (this.m_fetching) return;

        this.providerSources.valueX.ready = false;
        this.providerSources.valueY.ready = false;
        if (this.providerSources.valueZ) this.providerSources.valueZ.ready = false;

        try
        {
            this.transitionToActive();

            // Start with a small batch, to quickly refresh the UI.
            let maxSamples = 1000;

            let expectedDelta = this.maxGapForNewRange;

            if (this.source.expectedSamplingRate > 0)
            {
                expectedDelta = Math.max(expectedDelta, this.source.expectedSamplingRate);
            }

            while (true)
            {
                // todo: include alerts?
                let requests = [];
                requests.push(this.providerSources.valueX.request);
                requests.push(this.providerSources.valueY.request);
                if (this.providerSources.valueZ) requests.push(this.providerSources.valueZ.request);

                let series = await this.assets.getInterpolatedValues(requests, this.rangeStart, this.rangeEnd, maxSamples, this.maxGapForNewRange - 1);

                let gotNewSamples = false;

                let lookups        = series.results.map((result) => result.enumLookup);
                let lookupSets     = series.results.map((result) => result.enumSetLookup);
                let samplesArr     = series.results.map((result) => result.values);
                let dataSources    = [
                    this.source.dataSources.valueX,
                    this.source.dataSources.valueY
                ];
                let previousValues = [
                    this.m_previousValues?.valueX,
                    this.m_previousValues?.valueY
                ];
                if (series.results.length === 3)
                {
                    dataSources.push(this.source.dataSources.valueZ);
                    previousValues.push(this.m_previousValues?.valueZ);
                }

                let numSamples = series.timestamps.length;
                for (let i = 0; i < numSamples; i++)
                {
                    let timestamp = series.timestamps[i];

                    if (this.m_previousTimestamp !== undefined && timestamp <= this.m_previousTimestamp)
                    {
                        // For Metrics, we might reload the same data. Skip already seen values.
                        continue;
                    }

                    let preProcessedValues: number[] = [];
                    for (let j = 0; j < series.results.length; j++) preProcessedValues.push(samplesArr[j][i]);

                    let currValues: any[] = [];
                    let type: ChartPointType;
                    for (let j = 0; j < series.results.length; j++)
                    {
                        let lookup        = lookups[j];
                        let lookupSet     = lookupSets[j];
                        let dataSource    = dataSources[j];
                        let previousValue = previousValues[j];

                        let currValue: any = preProcessedValues[j];

                        if (currValue == dataSource.noValueMarker)
                        {
                            type      = ChartPointType.NoValue;
                            currValue = previousValue;
                        }
                        else if (isNaN(currValue))
                        {
                            if (!type || type === ChartPointType.Value) type = ChartPointType.Missing;
                            currValue = previousValue;
                        }
                        else
                        {
                            if (type == undefined) type = ChartPointType.Value;

                            if (lookup)
                            {
                                currValue = lookup[currValue];
                            }
                            else if (lookupSet)
                            {
                                currValue = lookupSet[currValue];
                            }
                        }

                        currValues[j] = currValue;
                    }

                    if (this.m_previousTimestamp !== undefined)
                    {
                        let delta = timestamp - this.m_previousTimestamp;

                        if (delta > this.m_previousDelta)
                        {
                            let nextTimestamp = series.timestamps[i + 1];
                            if (!nextTimestamp)
                            {
                                // Last sample, assume it's okay to put it in the current range.
                            }
                            else
                            {
                                let nextDelta = nextTimestamp - timestamp;

                                if (delta == nextDelta)
                                {
                                    // A change in sampling period, probably.
                                }
                                else
                                {
                                    let maxDelta = Math.max(3600, 2 * Math.max(this.m_previousDelta, nextDelta));

                                    if (delta > Math.max(maxDelta, expectedDelta))
                                    {
                                        // Create a new range.
                                        this.m_pointRange        = null;
                                        this.m_previousTimestamp = undefined;
                                    }
                                }
                            }
                        }

                        this.m_previousDelta = delta;
                    }

                    if (this.m_pointRange == null)
                    {
                        this.m_pointRange = this.source.addRange();
                    }

                    this.m_previousTimestamp = timestamp;
                    this.m_previousValues    = new ScatterPlotPropertyTuple(timestamp * 1_000, currValues[0], currValues[1], currValues[2]);

                    if (type === ChartPointType.Value) this.m_pointRange.addPoint(this.m_previousValues.timestamp, this.m_previousValues, type);
                    gotNewSamples = true;
                }

                if (!gotNewSamples)
                {
                    break;
                }

                let lastTimestamp = series.timestamps[numSamples - 1];
                let nextTimestamp = MomentHelper.parse((lastTimestamp + 1) * 1_000);

                if (this.m_subscriber)
                {
                    this.m_subscriber.newSamples(nextTimestamp);
                }

                this.m_lastTimestamp = nextTimestamp;

                maxSamples = Math.min(3 * maxSamples, 50000);
            }
        }
        catch (e)
        {
            // Ignore failures.
        }
        finally
        {
            this.transitionToInactive();
        }
    }

    private transitionToActive()
    {
        if (!this.m_fetching)
        {
            this.m_fetching = true;
            if (this.m_subscriber) this.m_subscriber.transitionToActive();
        }
    }

    private transitionToInactive()
    {
        if (this.m_fetching)
        {
            this.m_fetching = false;
            if (this.m_subscriber) this.m_subscriber.transitionToInactive();
        }
    }
}

class ChartPointProviderImpl extends ChartPointProvider
{
    private m_dbSubscription: SharedSvc.DbChangeSubscription<Models.Asset>;

    private m_subscriber: ChartPointSubscriber;
    private m_lastTimestampForAlerts: moment.Moment;
    private m_lastTimestampForSamples: moment.Moment;

    private m_pointRange: ChartPointRange<any> = null;
    private m_previousTimestamp: number        = undefined;
    private m_previousValue: any               = undefined;
    private m_previousDelta                    = 0;
    private m_fetching                         = false;

    constructor(private comp: SharedSvc.BaseApplicationComponent,
                private element: DeviceElementExtended,
                private property: string,
                source: ChartPointSource<any>,
                private includeAlerts: boolean,
                rangeStart: moment.Moment,
                rangeEnd: moment.Moment,
                public readonly timeOffset: Models.TimeDuration,
                private units?: Models.EngineeringUnitsFactors,
                private maxGapForNewRange: number = 30 * 60)
    {
        super(rangeStart, rangeEnd, source);
    }

    public startStreamingSamples(maxSamplesPerBatch: number,
                                 subscriber: ChartPointSubscriber)
    {
        if (!this.m_dbSubscription)
        {
            this.m_subscriber = subscriber;

            this.m_dbSubscription = this.comp.subscribe(this.element,
                                                        async (ext,
                                                               action) =>
                                                        {
                                                            await this.fetchSamples();
                                                        });

            this.fetchSamples();
        }
    }

    public stopStreamingSamples()
    {
        if (this.m_subscriber)
        {
            this.transitionToInactive();

            this.m_subscriber = null;
        }

        if (this.m_dbSubscription)
        {
            this.comp.removeSubscription(this.m_dbSubscription);
            this.m_dbSubscription = null;
        }
    }

    public accessContext<T>(type: Type<T>): T
    {
        if (this.element instanceof type) return this.element;

        return null;
    }

    public async prepareContextMenu(point: ChartPoint<any>,
                                    root: ContextMenuItemComponent): Promise<boolean>
    {
        let parent = await this.element.getParent();

        root.addItem("Navigate to Control Point", async () =>
        {
            await this.comp.app.ui.navigation.go(`/devices/device/${parent.model.sysId}/element/${this.element.model.sysId}`);
        });

        if (parent instanceof LogicalAssetExtended)
        {
            root.addItem("Navigate to Equipment", async () =>
            {
                await this.comp.app.ui.navigation.go(`/equipment/equipment/${parent.model.sysId}`);
            });
        }
        else
        {
            root.addItem("Navigate to Device", async () =>
            {
                await this.comp.app.ui.navigation.go(`/devices/device/${parent.model.sysId}`);
            });
        }

        return true;
    }

    //--//

    private transitionToActive()
    {
        if (!this.m_fetching)
        {
            this.m_fetching = true;
            if (this.m_subscriber) this.m_subscriber.transitionToActive();
        }
    }

    private transitionToInactive()
    {
        if (this.m_fetching)
        {
            this.m_fetching = false;
            if (this.m_subscriber) this.m_subscriber.transitionToInactive();
        }
    }

    public async computeMin(rangeStart: moment.Moment,
                            rangeEnd: moment.Moment): Promise<number>
    {
        return 0;
    }

    public async computeMax(rangeStart: moment.Moment,
                            rangeEnd: moment.Moment): Promise<number>
    {
        return 0;
    }

    public async computeSum(rangeStart: moment.Moment,
                            rangeEnd: moment.Moment): Promise<number>
    {
        let series = await this.element.getValues(this.property, rangeStart, rangeEnd, undefined, undefined, undefined, undefined, undefined, this.units);

        let sum = 0;

        if (series && series.results && series.results.values)
        {
            for (let value of series.results.values)
            {
                sum += value;
            }
        }

        return sum;
    }

    //--//

    private async fetchSamples(): Promise<void>
    {
        if (this.m_fetching)
        {
            return;
        }

        this.transitionToActive();

        try
        {
            await this.fetchNewSamples();
            await this.fetchNewAlerts();
        }
        catch (e)
        {
            // Ignore failures.
        }

        this.transitionToInactive();
    }

    private async fetchNewSamples(): Promise<void>
    {
        try
        {
            // Start with a small batch, to quickly refresh the UI.
            let maxSamples = 1000;

            let expectedDelta = this.maxGapForNewRange;

            if (this.source.expectedSamplingRate > 0)
            {
                expectedDelta = Math.max(expectedDelta, this.source.expectedSamplingRate);
            }

            while (true)
            {
                let series = await this.element.getValues(this.property,
                                                          this.rangeStart,
                                                          this.rangeEnd,
                                                          this.timeOffset,
                                                          3600,
                                                          this.m_lastTimestampForSamples,
                                                          maxSamples,
                                                          this.maxGapForNewRange - 1,
                                                          this.units);

                let gotNewSamples = false;

                let lookup    = series.results.enumLookup;
                let lookupSet = series.results.enumSetLookup;
                let samples   = series.results.values;

                let numSamples = series.timestamps.length;
                for (let i = 0; i < numSamples; i++)
                {
                    let timestamp = series.timestamps[i];

                    if (this.m_previousTimestamp !== undefined && timestamp <= this.m_previousTimestamp)
                    {
                        // For Metrics, we might reload the same data. Skip already seen values.
                        continue;
                    }

                    let value: any;
                    let type: ChartPointType;

                    value = samples[i];

                    if (value == this.source.noValueMarker)
                    {
                        type  = ChartPointType.NoValue;
                        value = this.m_previousValue;
                    }
                    else if (isNaN(value))
                    {
                        type  = ChartPointType.Missing;
                        value = this.m_previousValue;
                    }
                    else
                    {
                        type = ChartPointType.Value;

                        if (lookup)
                        {
                            value = lookup[value];
                        }
                        else if (lookupSet)
                        {
                            value = lookupSet[value];
                        }
                    }

                    if (this.m_previousTimestamp !== undefined)
                    {
                        let delta = timestamp - this.m_previousTimestamp;

                        if (delta > this.m_previousDelta)
                        {
                            let nextTimestamp = series.timestamps[i + 1];
                            if (!nextTimestamp)
                            {
                                // Last sample, assume it's okay to put it in the current range.
                            }
                            else
                            {
                                let nextDelta = nextTimestamp - timestamp;

                                if (delta == nextDelta)
                                {
                                    // A change in sampling period, probably.
                                }
                                else
                                {
                                    let maxDelta = Math.max(3600, 2 * Math.max(this.m_previousDelta, nextDelta));

                                    if (delta > Math.max(maxDelta, expectedDelta))
                                    {
                                        // Create a new range.
                                        this.m_pointRange        = null;
                                        this.m_previousTimestamp = undefined;
                                    }
                                }
                            }
                        }

                        this.m_previousDelta = delta;
                    }

                    if (this.m_pointRange == null)
                    {
                        this.m_pointRange = this.source.addRange();
                    }

                    this.m_previousTimestamp = timestamp;
                    this.m_previousValue     = value;

                    this.m_pointRange.addPoint(timestamp * 1_000, value, type);
                    gotNewSamples = true;
                }

                if (!gotNewSamples)
                {
                    break;
                }

                let nextTimestamp: moment.Moment;

                if (series.results.nextTimestamp)
                {
                    nextTimestamp = MomentHelper.parse(series.results.nextTimestamp);
                }
                else
                {
                    let lastTimestamp = series.timestamps[numSamples - 1];
                    nextTimestamp     = MomentHelper.parse((lastTimestamp + 1) * 1_000);
                }

                if (this.m_subscriber)
                {
                    this.m_subscriber.newSamples(nextTimestamp);
                }

                this.m_lastTimestampForSamples = nextTimestamp;

                if (!series.results.nextTimestamp)
                {
                    break;
                }

                maxSamples = Math.min(3 * maxSamples, 50000);
            }
        }
        catch (e)
        {
            // Ignore failures.
        }
    }

    private async fetchNewAlerts(): Promise<void>
    {
        try
        {
            if (this.includeAlerts)
            {
                if (!this.m_lastTimestampForAlerts)
                {
                    this.m_lastTimestampForAlerts = this.rangeStart;
                }

                let goodNewAlerts = false;

                let alerts = await this.element.getAlertHistory(this.m_lastTimestampForAlerts, this.rangeEnd);
                for (let alert of alerts)
                {
                    let details          = await alert.extractDetails();
                    let marker           = new AlertMarker(this.comp.app, details, this.source);
                    marker.sizeStrategy  = AlertSizeStrategy.ALERT_REPORT;
                    marker.colorStrategy = AlertColorStrategy.ALERT_REPORT;
                    marker.shapeStrategy = AlertShapeStrategy.ALERT_REPORT;
                    this.source.markers.push(marker);

                    this.m_lastTimestampForAlerts = MomentHelper.max(this.m_lastTimestampForAlerts, details.eventTimestamp);
                    goodNewAlerts                 = true;
                }

                if (goodNewAlerts && this.m_subscriber)
                {
                    this.m_subscriber.newSamples(this.m_lastTimestampForSamples);
                }
            }
        }
        catch (e)
        {
            // Ignore failures.
        }
    }
}

export interface DeviceElementSchema
{
    [key: string]: Models.TimeSeriesPropertyType;
}

export interface EquipmentSummary extends ILazyTreeNode<string>
{
    name: string;
    equipmentClassId: string;
    locationName: string;
}
