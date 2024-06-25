import {Injectable, Type} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import {AssetExtended, DeviceElementExtended, LogicalAssetExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import {ReportDefinitionVersionExtended} from "app/services/domain/report-definition-versions.service";
import {ReportConfigurationExtended, ReportSchedulingOptionsExtended} from "app/services/domain/reporting.service";
import {SearchResult} from "app/services/domain/search.service";
import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";

import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class ReportDefinitionsService extends SharedSvc.BaseService<Models.ReportDefinition, ReportDefinitionExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.ReportDefinition, ReportDefinitionExtended.newInstance);
    }

    @ReportError
    public getList(filters: Models.ReportDefinitionFilterRequest): Promise<Models.RecordIdentity[]>
    {
        return this.api.reportDefinitions.getFiltered(filters);
    }

    //--//

    protected cachePrefix(): string { return Models.ReportDefinition.RECORD_IDENTITY; }

    protected getRaw(id: string): Promise<Models.ReportDefinition>
    {
        return this.api.reportDefinitions.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.ReportDefinition[]>
    {
        return this.api.reportDefinitions.getBatch(ids);
    }
}

export class ReportDefinitionExtended extends SharedSvc.ExtendedModel<Models.ReportDefinition>
{
    static newInstance(svc: ReportDefinitionsService,
                       model: Models.ReportDefinition): ReportDefinitionExtended
    {
        return new ReportDefinitionExtended(svc, model, Models.ReportDefinition.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.ReportDefinition.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    async save(): Promise<ReportDefinitionExtended>
    {
        let svc = this.domain.reportDefinitions;

        if (this.model.sysId)
        {
            await svc.flush(this);

            await this.domain.apis.reportDefinitions.update(this.model.sysId, undefined, this.model);
            return this.refresh();
        }
        else
        {
            let newModel = await this.domain.apis.reportDefinitions.create(this.model);
            return svc.wrapModel(newModel);
        }
    }

    /**
     * Get last edited version
     */
    async getHead(): Promise<ReportDefinitionVersionExtended>
    {
        return this.domain.reportDefinitionVersions.getExtendedByIdentity(this.model.headVersion);
    }

    /**
     * Get current active version
     */
    async getRelease(): Promise<ReportDefinitionVersionExtended>
    {
        return this.domain.reportDefinitionVersions.getExtendedByIdentity(this.model.releaseVersion);
    }

    async getAllVersions(): Promise<ReportDefinitionVersionExtended[]>
    {
        let versions = await this.domain.reportDefinitionVersions.getExtendedBatch(this.model.versions);
        versions.sort((a,
                       b) => b.model.version - a.model.version);
        return versions;
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        if (!this.model.sysId) return new Models.ValidationResults();

        let svc = this.domain.reportDefinitions;
        svc.errors.dismiss();

        await svc.flush(this);
        return svc.api.reportDefinitions.remove(this.model.sysId);
    }
}

export class ReportDefinitionDetailsExtended
{
    constructor(public domain: AppDomainContext,
                public model: Models.ReportDefinitionDetails)
    {
        this.init();
    }

    get typedModel(): Models.ReportDefinitionDetails
    {
        return this.model;
    }

    private init()
    {
        if (!this.model.schedule)
        {
            this.model.schedule = Models.ReportSchedulingOptions.newInstance({
                                                                                 range: Models.TimeRangeId.Last24Hours
                                                                             });
        }
    }

    public getOrCreateConfig(): ReportConfigurationExtended
    {
        let raw = this.typedModel.reportConfiguration;
        if (!raw)
        {
            raw = Models.ReportConfiguration.newInstance({sharedGraphs: []});
        }

        return new ReportConfigurationExtended(raw);
    }

    @Memoizer
    public getSchedulingOptions()
    {
        return new ReportSchedulingOptionsExtended(this.typedModel.schedule, this.domain, true);
    }
}

export abstract class AssetSelectionExtended<E extends AssetExtended>
{
    private distinct: Map<string, Models.RecordIdentity> = new Map<string, Models.RecordIdentity>();

    constructor(protected domain: AppDomainContext,
                private m_identities: Models.RecordIdentity[] = [])
    {
        this.updateDistinct();
    }

    get identities(): Models.RecordIdentity[]
    {
        return this.m_identities;
    }

    setIdentities(identities: Models.RecordIdentity[])
    {
        this.m_identities = identities || [];
        this.updateDistinct();
    }

    get count(): number
    {
        return this.identities.length;
    }

    select(record: Models.RecordIdentity)
    {
        if (!this.distinct.has(record.sysId))
        {
            this.identities.push(record);
            this.distinct.set(record.sysId, record);
        }
    }

    deselect(record: Models.RecordIdentity)
    {
        if (this.distinct.has(record.sysId))
        {
            let original = this.distinct.get(record.sysId);
            let index    = this.identities.indexOf(original);

            if (index >= 0) this.identities.splice(index, 1);
            this.distinct.delete(record.sysId);
        }
    }

    async getRecords(type: Type<E>,
                     identities?: Models.RecordIdentity[]): Promise<E[]>
    {
        identities = identities || this.identities;

        return await this.domain.assets.getTypedExtendedBatch(type, identities);
    }

    abstract getRecordsAsSearchResults(): Promise<SearchResult[]>;

    public abstract getDisplayText(singleMode: boolean,
                                   readonly: boolean): string;

    private updateDistinct()
    {
        this.distinct = new Map<string, Models.RecordIdentity>();
        for (let record of this.identities) this.distinct.set(record.sysId, record);
    }
}

export class ControlPointsSelectionExtended extends AssetSelectionExtended<DeviceElementExtended>
{
    constructor(domain: AppDomainContext,
                private options: Models.ControlPointsSelection     = new Models.ControlPointsSelection(),
                private m_category: ControlPointsSelectionCategory = ControlPointsSelectionCategory.All)
    {
        super(domain, options.identities || []);

        if (!this.options.identities)
        {
            this.options.identities = this.identities;
        }
    }

    public setIdentities(identities: Models.RecordIdentity[])
    {
        super.setIdentities(identities);
        this.options.identities = this.identities;
    }

    get model(): Models.ControlPointsSelection
    {
        return this.options;
    }

    set category(id: string)
    {
        this.m_category = ControlPointsSelectionCategory.getById(id);
    }

    get category(): string
    {
        return this.m_category.id;
    }

    getSearchText(searchText: string): string
    {
        return `${this.getUnitsSearchText()} ${searchText && searchText.trim() || ""}`.trim();
    }

    private getUnitsSearchText(): string
    {
        if (!this.m_category)
        {
            return "";
        }

        if (this.m_category.units && this.m_category.units.length)
        {
            return `(${this.m_category.units.join(" | ")})`;
        }

        return "";
    }

    public getDisplayText(singleMode: boolean = false,
                          readonly: boolean   = false)
    {
        let length = this.identities.length;

        if (length)
        {
            return `${length} point${length > 1 ? "s" : ""}`;
        }

        if (readonly)
        {
            return `No Control Point${singleMode ? "" : "s"} selected`;
        }

        return `Select Control Point${singleMode ? "" : "s"}`;
    }

    public async getRecordsAsSearchResults(): Promise<SearchResult[]>
    {
        let records                 = await this.getRecords(DeviceElementExtended);
        let results: SearchResult[] = [];
        await this.domain.search.getDeviceElementsResults(records, results);
        return results;
    }
}

export class EquipmentSelectionExtended extends AssetSelectionExtended<LogicalAssetExtended>
{
    constructor(domain: AppDomainContext,
                m_identities: Models.RecordIdentity[] = [])
    {
        super(domain, m_identities);
    }

    public getDisplayText(singleMode: boolean = false,
                          readonly: boolean   = false)
    {
        let length = this.identities.length;

        if (length)
        {
            return `${length} equipment`;
        }

        if (readonly)
        {
            return `No equipment selected`;
        }

        return `Select equipment`;
    }

    public async getRecordsAsSearchResults(): Promise<SearchResult[]>
    {
        let records                 = await this.getRecords(LogicalAssetExtended);
        let results: SearchResult[] = [];
        await this.domain.search.getEquipmentResults(records, results);
        return results;
    }
}

export class ControlPointsSelectionCategory
{
    private static s_allCategories: ControlPointsSelectionCategory[] = [];
    static All                                                       = new ControlPointsSelectionCategory("All", "All", null);
    static AirFlow                                                   = new ControlPointsSelectionCategory("AirFlow", "Air Flow", ["cubic_feet_per_minute"]);
    static AirQuality                                                = new ControlPointsSelectionCategory("AirQuality", "Air Quality", ["parts_per_million"]);
    static EnergyUsage                                               = new ControlPointsSelectionCategory("EnergyUsage", "Energy Usage", ["watt"]);
    static Temperature                                               = new ControlPointsSelectionCategory("Temperature", "Temperature", [
        "degrees_fahrenheit",
        "degrees_celsius"
    ]);
    static WaterUsage                                                = new ControlPointsSelectionCategory("WaterUsage", "Water Usage", ["us_gallons_per_minute"]);

    public static getAllCategories(): ControlPointsSelectionCategory[]
    {
        return this.s_allCategories;
    }

    public static getById(id: string): ControlPointsSelectionCategory
    {
        return (<any>ControlPointsSelectionCategory)[id];
    }

    private constructor(private m_id: string,
                        private m_display: string,
                        private m_units: string[])
    {
        ControlPointsSelectionCategory.s_allCategories.push(this);
    }

    public get displayName()
    {
        return this.m_display;
    }

    public get id()
    {
        return this.m_id;
    }

    public get units()
    {
        return this.m_units;
    }
}
