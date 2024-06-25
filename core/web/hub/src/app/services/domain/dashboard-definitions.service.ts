import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {DashboardDefinitionVersionExtended} from "app/services/domain/dashboard-definition-versions.service";
import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";

@Injectable()
export class DashboardDefinitionsService extends SharedSvc.BaseService<Models.DashboardDefinition, DashboardDefinitionExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.DashboardDefinition, DashboardDefinitionExtended.newInstance);
    }

    @ReportError
    public getAll(): Promise<Models.RecordIdentity[]>
    {
        return this.api.dashboardDefinitions.getAll();
    }

    @ReportError
    async getExtendedAll(): Promise<DashboardDefinitionExtended[]>
    {
        let ids = await this.getAll();
        let all = await this.getExtendedBatch(ids);
        return all;
    }

    //--//

    protected cachePrefix(): string { return Models.DashboardDefinition.RECORD_IDENTITY; }

    protected getRaw(id: string): Promise<Models.DashboardDefinition>
    {
        return this.api.dashboardDefinitions.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.DashboardDefinition[]>
    {
        return this.api.dashboardDefinitions.getBatch(ids);
    }
}

export class DashboardDefinitionExtended extends SharedSvc.ExtendedModel<Models.DashboardDefinition>
{
    static newInstance(svc: DashboardDefinitionsService,
                       model: Models.DashboardDefinition): DashboardDefinitionExtended
    {
        return new DashboardDefinitionExtended(svc, model, Models.DashboardDefinition.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.DashboardDefinition.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    async save(): Promise<DashboardDefinitionExtended>
    {
        let svc = this.domain.dashboardDefinitions;

        if (this.model.sysId)
        {
            await svc.flush(this);

            await this.domain.apis.dashboardDefinitions.update(this.model.sysId, undefined, this.model);
            return this.refresh();
        }
        else
        {
            let newModel = await this.domain.apis.dashboardDefinitions.create(this.model);
            return svc.wrapModel(newModel);
        }
    }

    /**
     * Get last edited version
     */
    async getHead(): Promise<DashboardDefinitionVersionExtended>
    {
        return this.domain.dashboardDefinitionVersions.getExtendedByIdentity(this.model.headVersion);
    }

    /**
     * Get current active version
     */
    async getRelease(): Promise<DashboardDefinitionVersionExtended>
    {
        return this.domain.dashboardDefinitionVersions.getExtendedByIdentity(this.model.releaseVersion);
    }

    async getVersion(id: Models.RecordIdentity): Promise<DashboardDefinitionVersionExtended>
    {
        return this.domain.dashboardDefinitionVersions.getExtendedByIdentity(id);
    }

    async getAllVersions(): Promise<DashboardDefinitionVersionExtended[]>
    {
        let id = this.getIdentity();

        let versions = await this.domain.apis.dashboardDefinitions.getHistory(this.model.sysId);
        versions.sort((a,
                       b) => b.version - a.version);
        return versions.map((ver) =>
                            {
                                ver.definition = id;
                                return this.domain.dashboardDefinitionVersions.wrapModel(ver);
                            });
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        if (!this.model.sysId) return new Models.ValidationResults();

        let svc = this.domain.dashboardDefinitions;
        svc.errors.dismiss();

        await svc.flush(this);
        return svc.api.dashboardDefinitions.remove(this.model.sysId);
    }

    //--//

    async linkNewToHead(model: Models.DashboardConfiguration,
                        predecessorId?: Models.RecordIdentity): Promise<DashboardDefinitionVersionExtended>
    {
        let verExt;
        if (predecessorId) verExt = await this.getVersion(predecessorId);
        if (!verExt) verExt = await this.getHead();

        if (!verExt)
        {
            verExt                  = this.domain.dashboardDefinitionVersions.allocateInstance();
            verExt.model.definition = this.getIdentity();
        }
        else
        {
            verExt = verExt.getNewVersion();
        }

        verExt.model.details = model;

        return await verExt.save();
    }
}
