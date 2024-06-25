import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {NormalizationRulesBlocklyWorkspaceData} from "app/customer/configuration/classification/normalization-rules-blockly-workspace.data";
import {BlocklyWorkspaceData} from "app/customer/engines/shared/workspace-data";

import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";

@Injectable()
export class NormalizationService extends SharedSvc.BaseService<Models.Normalization, NormalizationExtended>
{
    private m_lastActive: Models.Normalization;
    private m_notifier: SharedSvc.DbChangeNotifier<Models.Normalization, NormalizationExtended>;

    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.Normalization, NormalizationExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.Normalization.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.Normalization>
    {
        return this.api.normalization.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.Normalization[]>
    {
        return this.api.normalization.getBatch(ids);
    }

    protected canCacheValues(): boolean
    {
        // Normalization records are big, we only cache the active one.
        return false;
    }

    //--//

    @ReportError
    public getList(): Promise<Models.RecordIdentity[]>
    {
        return this.api.normalization.getAll();
    }

    @ReportError
    public async getActiveRules(): Promise<Models.Normalization>
    {
        if (this.m_lastActive)
        {
            return this.m_lastActive;
        }

        let activeId = await this.api.normalization.getActive();
        let active   = await this.getWithIdentity(activeId);
        if (active)
        {
            this.m_lastActive = active;
        }

        if (!this.m_notifier)
        {
            this.m_notifier = this.listenToDatabase(async (from,
                                                           action) =>
                                                    {
                                                        this.m_lastActive = null;
                                                    });
        }

        return active;
    }

    public async getWellKnownEquipmentClassId(wellKnownEquipmentClass: Models.WellKnownEquipmentClass): Promise<string>
    {
        let normalization = await this.getActiveRules();
        let classId = normalization.rules.equipmentClasses.find((ec) => ec.wellKnown === wellKnownEquipmentClass)?.id;
        return classId ? `${classId}` : null;
    }

    public async getWellKnownPointClassId(wellKnownPointClass: Models.WellKnownPointClass): Promise<string>
    {
        let normalization = await this.getActiveRules();
        let classId = normalization.rules.pointClasses.find((pc) => pc.wellKnown === wellKnownPointClass)?.id;
        return classId ? `${classId}` : null;
    }

    //--//

    public async getExtendedAll(): Promise<NormalizationExtended[]>
    {
        let ids = await this.getList();
        return this.getExtendedBatch(ids);
    }

    //--//

    public async parseImportedRules(contents: string): Promise<Models.NormalizationRules>
    {
        let raw = Models.RawImport.newInstance({contentsAsJSON: contents});
        return await this.api.normalization.parseImport(raw);
    }

    public async parseImportedOverrides(contents: string): Promise<Models.DeviceElementClassificationOverridesRequest[]>
    {
        let raw = Models.RawImport.newInstance({contentsAsJSON: contents});
        return this.api.normalization.parseOverridesImport(raw);
    }

    async evaluate(sample: Models.DeviceElementNormalizationSample,
                   maxSteps?: number,
                   trace?: boolean): Promise<Models.NormalizationEvaluation>
    {
        return await this.api.normalization.evaluate(sample, maxSteps, trace);
    }

    @ReportError
    loadSample(sysId: string): Promise<Models.DeviceElementNormalizationSample>
    {
        return this.api.normalization.loadSample((sysId ?? "").trim());
    }
}

export class NormalizationExtended extends SharedSvc.ExtendedModel<Models.Normalization>
{
    static newInstance(svc: NormalizationService,
                       model: Models.Normalization): NormalizationExtended
    {
        return new NormalizationExtended(svc, model, Models.Normalization.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.Normalization.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    getDetailsExtended<T extends NormalizationDefinitionDetailsExtended>()
    {
        let details = this.model.rules.logic;
        if (!details)
        {
            details = new Models.NormalizationDefinitionDetailsForUserProgram();
        }

        return <T>(NormalizationDefinitionDetailsExtended.newInstance(this.domain, details, this.model.rules));
    }

    setDetailsExtended<T extends NormalizationDefinitionDetailsExtended>(ext: T)
    {
        this.model.rules.logic = ext.typedModel;
    }

    evaluate(sample: Models.DeviceElementNormalizationSample,
             maxSteps?: number,
             trace?: boolean): Promise<Models.NormalizationEvaluation>
    {
        return this.domain.normalization.evaluate(sample, maxSteps, trace);
    }
}

export type NormalizationChangeSubscription = SharedSvc.DbChangeSubscription<Models.Normalization>;

//--//

export abstract class NormalizationDefinitionDetailsExtended
{
    static newInstance(domain: AppDomainContext,
                       details: Models.NormalizationDefinitionDetails,
                       rules: Models.NormalizationRules): NormalizationDefinitionDetailsExtended
    {
        if (details instanceof Models.NormalizationDefinitionDetailsForUserProgram)
        {
            return new NormalizationDefinitionDetailsForUserProgramExtended(domain, details, rules);
        }

        return null;
    }

    public data: BlocklyWorkspaceData;

    constructor(public domain: AppDomainContext,
                public model: Models.NormalizationDefinitionDetails,
                public rules: Models.NormalizationRules)
    {
        this.init();
        this.data = new NormalizationRulesBlocklyWorkspaceData(this.model.tabs, this.rules);
    }

    get typedModel(): Models.NormalizationDefinitionDetails
    {
        return this.model;
    }

    protected abstract init(): void;
}

export class NormalizationDefinitionDetailsForUserProgramExtended extends NormalizationDefinitionDetailsExtended
{
    get typedModel(): Models.NormalizationDefinitionDetailsForUserProgram
    {
        return <Models.NormalizationDefinitionDetailsForUserProgram>this.model;
    }

    protected init(): void
    {
        if (!this.typedModel.tabs)
        {
            this.typedModel.tabs = [];
        }
    }
}
