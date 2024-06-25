import {Injectable} from "@angular/core";
import {ReportError} from "app/app.service";
import {AlertRuleBlocklyWorkspaceData, AlertRuleConfigurationValues} from "app/customer/configuration/alert-rules/wizard/alert-rule-blockly-workspace-data";
import {BlocklyWorkspaceData} from "app/customer/engines/shared/workspace-data";
import {AlertDefinitionVersionExtended} from "app/services/domain/alert-definition-versions.service";
import {ApiService} from "app/services/domain/api.service";
import {AssetGraphExtended} from "app/services/domain/asset-graph.service";
import * as SharedSvc from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";
import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";

@Injectable()
export class AlertDefinitionsService extends SharedSvc.BaseService<Models.AlertDefinition, AlertDefinitionExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.AlertDefinition, AlertDefinitionExtended.newInstance);
    }

    @ReportError
    public getList(filters?: Models.AlertDefinitionFilterRequest): Promise<Models.RecordIdentity[]>
    {
        return this.api.alertDefinitions.getFiltered(filters);
    }

    @ReportError
    public async getExtendedList(filters?: Models.AlertDefinitionFilterRequest): Promise<AlertDefinitionExtended[]>
    {
        let ids = await this.getList(filters);
        let all = await this.getExtendedBatch(ids);
        return all;
    }

    //--//

    protected cachePrefix(): string { return Models.AlertDefinition.RECORD_IDENTITY; }

    protected getRaw(id: string): Promise<Models.AlertDefinition>
    {
        return this.api.alertDefinitions.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.AlertDefinition[]>
    {
        return this.api.alertDefinitions.getBatch(ids);
    }

    /**
     * Create or update the alert definition.
     */
    @ReportError
    async save(model: Models.AlertDefinition): Promise<Models.AlertDefinition>
    {
        if (model.sysId)
        {
            this.flushModel(model);

            await this.api.alertDefinitions.update(model.sysId, undefined, model);
            return this.get(model.sysId, model.updatedOn);
        }
        else
        {
            return await this.api.alertDefinitions.create(model);
        }
    }
}

export class AlertDefinitionExtended extends SharedSvc.ExtendedModel<Models.AlertDefinition>
{
    static newInstance(svc: AlertDefinitionsService,
                       model: Models.AlertDefinition): AlertDefinitionExtended
    {
        return new AlertDefinitionExtended(svc, model, Models.AlertDefinition.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.AlertDefinition.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    async save(): Promise<AlertDefinitionExtended>
    {
        let validations = this.validate();

        if (!validations.length)
        {
            // save entity
            this.model = await this.domain.alertDefinitions.save(this.model);

            return this;
        }
        else
        {
            this.domain.events.errors.error("VALIDATION_ERRORS", "Alert Definition could not be saved.", validations.map((a) => a.message));
            throw new Error("VALIDATION_ERRORS");
        }
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        if (!this.model.sysId) return new Models.ValidationResults();

        let svc = this.domain.alertDefinitions;
        svc.errors.dismiss();

        await svc.flush(this);
        return svc.api.alertDefinitions.remove(this.model.sysId);
    }

    filterLog(filters: Models.LogEntryFilterRequest): Promise<Models.LogRange[]>
    {
        return this.domain.apis.alertDefinitions.filterLog(this.model.sysId, filters);
    }

    getLog(fromOffset?: number,
           toOffset?: number,
           limit?: number): Promise<Models.LogLine[]>
    {
        return this.domain.apis.alertDefinitions.getLog(this.model.sysId, fromOffset, toOffset, limit);
    }

    deleteLog(olderThan?: number): Promise<number>
    {
        return this.domain.apis.alertDefinitions.deleteLog(this.model.sysId, olderThan);
    }

    /**
     * Get last edited version
     */
    async getHead(): Promise<AlertDefinitionVersionExtended>
    {
        return this.domain.alertDefinitionVersions.getExtendedByIdentity(this.model.headVersion);
    }

    /**
     * Get current active version
     */
    async getRelease(): Promise<AlertDefinitionVersionExtended>
    {
        return this.domain.alertDefinitionVersions.getExtendedByIdentity(this.model.releaseVersion);
    }

    async getAllVersions(): Promise<AlertDefinitionVersionExtended[]>
    {
        let versions = await this.domain.alertDefinitionVersions.getExtendedBatch(this.model.versions);
        versions.sort((a,
                       b) => b.model.version - a.model.version);
        return versions;
    }

    getExport(): Models.AlertDefinition
    {
        return Models.AlertDefinition.newInstance({
                                                      title      : this.model.title,
                                                      purpose    : this.model.purpose,
                                                      description: this.model.description,
                                                      active     : this.model.active
                                                  });
    }
}

export abstract class AlertDefinitionDetailsExtended
{
    static newInstance(configValues: AlertRuleConfigurationValues,
                       domain: AppDomainContext,
                       details: Models.AlertDefinitionDetails): AlertDefinitionDetailsExtended
    {
        if (details instanceof Models.AlertDefinitionDetailsForUserProgram)
        {
            return new AlertDefinitionDetailsForUserProgramExtended(configValues, domain, details);
        }

        return null;
    }

    public data: BlocklyWorkspaceData;

    private m_graph: AssetGraphExtended;

    constructor(configValues: AlertRuleConfigurationValues,
                private m_domain: AppDomainContext,
                public model: Models.AlertDefinitionDetails)
    {
        this.init();
        this.data = new AlertRuleBlocklyWorkspaceData(configValues, () => this.graph.getNodesForDropdown(), this.model.tabs);
    }

    get typedModel(): Models.AlertDefinitionDetails
    {
        return this.model;
    }

    get graph(): AssetGraphExtended
    {
        if (!this.m_graph)
        {
            if (!this.model.graph)
            {
                this.model.graph = new Models.AssetGraph();
            }

            this.m_graph = new AssetGraphExtended(this.m_domain, this.model.graph);
        }

        return this.m_graph;
    }

    get graphModel(): Models.AssetGraph
    {
        return this.model.graph;
    }

    set graphModel(graphModel: Models.AssetGraph)
    {
        this.model.graph      = graphModel;
        this.m_graph          = null;
        this.graph.isPristine = false;
    }

    public get isPristine(): boolean
    {
        return !this.m_graph || this.m_graph.isPristine;
    }

    public markPristine()
    {
        if (this.m_graph)
        {
            this.m_graph.isPristine = true;
        }
    }

    protected abstract init(): void;
}

export class AlertDefinitionDetailsForUserProgramExtended extends AlertDefinitionDetailsExtended
{
    get typedModel(): Models.AlertDefinitionDetailsForUserProgram
    {
        return <Models.AlertDefinitionDetailsForUserProgram>this.model;
    }

    protected init(): void
    {
        if (!this.typedModel.tabs)
        {
            this.typedModel.tabs = [];
        }
    }
}
