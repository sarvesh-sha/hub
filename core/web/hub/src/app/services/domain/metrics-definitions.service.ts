import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {MetricBlocklyWorkspaceData, MetricConfigurationValues} from "app/customer/configuration/metrics/wizard/metric-blockly-workspace-data";
import {BlocklyWorkspaceData} from "app/customer/engines/shared/workspace-data";
import {ApiService} from "app/services/domain/api.service";
import {AssetGraphExtended} from "app/services/domain/asset-graph.service";
import * as SharedSvc from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import {MetricsDefinitionVersionExtended} from "app/services/domain/metrics-definition-versions.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";

@Injectable()
export class MetricsDefinitionsService extends SharedSvc.BaseService<Models.MetricsDefinition, MetricsDefinitionExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.MetricsDefinition, MetricsDefinitionExtended.newInstance);
    }

    @ReportError
    public getList(filters?: Models.MetricsDefinitionFilterRequest): Promise<Models.RecordIdentity[]>
    {
        return this.api.metricsDefinitions.getFiltered(filters);
    }

    @ReportError
    public async getExtendedList(filters?: Models.MetricsDefinitionFilterRequest): Promise<MetricsDefinitionExtended[]>
    {
        let ids = await this.getList(filters);
        let all = await this.getExtendedBatch(ids);
        return all;
    }

    //--//

    protected cachePrefix(): string { return Models.MetricsDefinition.RECORD_IDENTITY; }

    protected getRaw(id: string): Promise<Models.MetricsDefinition>
    {
        return this.api.metricsDefinitions.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.MetricsDefinition[]>
    {
        return this.api.metricsDefinitions.getBatch(ids);
    }

    /**
     * Create or update the metrics definition.
     */
    @ReportError
    async save(model: Models.MetricsDefinition,
               forceCreate?: boolean): Promise<Models.MetricsDefinition>
    {
        if (model.sysId && !forceCreate)
        {
            this.flushModel(model);

            await this.api.metricsDefinitions.update(model.sysId, undefined, model);
            return this.get(model.sysId, model.updatedOn);
        }
        else
        {
            return await this.api.metricsDefinitions.create(model);
        }
    }
}

export class MetricsDefinitionExtended extends SharedSvc.ExtendedModel<Models.MetricsDefinition>
{
    static newInstance(svc: MetricsDefinitionsService,
                       model: Models.MetricsDefinition): MetricsDefinitionExtended
    {
        return new MetricsDefinitionExtended(svc, model, Models.MetricsDefinition.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.MetricsDefinition.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    async save(forceCreate?: boolean): Promise<MetricsDefinitionExtended>
    {
        let validations = this.validate();

        if (!validations.length)
        {
            // save entity
            this.model = await this.domain.metricsDefinitions.save(this.model, forceCreate);

            return this;
        }
        else
        {
            this.domain.events.errors.error("VALIDATION_ERRORS", "Metrics Definition could not be saved.", validations.map((a) => a.message));
            throw new Error("VALIDATION_ERRORS");
        }
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        if (!this.model.sysId) return new Models.ValidationResults();

        let svc = this.domain.metricsDefinitions;
        svc.errors.dismiss();

        await svc.flush(this);
        return svc.api.metricsDefinitions.remove(this.model.sysId);
    }

    /**
     * Get last edited version
     */
    async getHead(): Promise<MetricsDefinitionVersionExtended>
    {
        return this.domain.metricsDefinitionVersions.getExtendedByIdentity(this.model.headVersion);
    }

    /**
     * Get current active version
     */
    async getRelease(): Promise<MetricsDefinitionVersionExtended>
    {
        return this.domain.metricsDefinitionVersions.getExtendedByIdentity(this.model.releaseVersion);
    }

    async getAllVersions(): Promise<MetricsDefinitionVersionExtended[]>
    {
        let versions = await this.domain.metricsDefinitionVersions.getExtendedBatch(this.model.versions);
        versions.sort((a,
                       b) => b.model.version - a.model.version);
        return versions;
    }

    getExport(): Models.MetricsDefinition
    {
        return Models.MetricsDefinition.newInstance({
                                                        sysId      : this.model.sysId,
                                                        title      : this.model.title,
                                                        description: this.model.description
                                                    });
    }
}

export abstract class MetricsDefinitionDetailsExtended
{
    static newInstance(configValues: MetricConfigurationValues,
                       domain: AppDomainContext,
                       details: Models.MetricsDefinitionDetails): MetricsDefinitionDetailsExtended
    {
        if (details instanceof Models.MetricsDefinitionDetailsForUserProgram)
        {
            return new MetricsDefinitionDetailsForUserProgramExtended(configValues, domain, details);
        }

        return null;
    }

    public data: BlocklyWorkspaceData;

    private m_graph: AssetGraphExtended;

    constructor(configValues: MetricConfigurationValues,
                private m_domain: AppDomainContext,
                public model: Models.MetricsDefinitionDetails)
    {
        this.init();
        this.data = new MetricBlocklyWorkspaceData(configValues, () => this.graph.getNodesForDropdown(), this.model.tabs);
    }

    get typedModel(): Models.MetricsDefinitionDetails
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

export class MetricsDefinitionDetailsForUserProgramExtended extends MetricsDefinitionDetailsExtended
{
    get typedModel(): Models.MetricsDefinitionDetailsForUserProgram
    {
        return <Models.MetricsDefinitionDetailsForUserProgram>this.model;
    }

    protected init(): void
    {
        if (!this.typedModel.tabs)
        {
            this.typedModel.tabs = [];
        }
    }
}
