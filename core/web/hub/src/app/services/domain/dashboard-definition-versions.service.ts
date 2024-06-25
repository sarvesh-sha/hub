import {Injectable} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {DashboardDefinitionExtended} from "app/services/domain/dashboard-definitions.service";

import * as Models from "app/services/proxy/model/models";
import {CacheService} from "framework/services/cache.service";

import {ErrorService} from "framework/services/error.service";

@Injectable()
export class DashboardDefinitionVersionsService extends SharedSvc.BaseService<Models.DashboardDefinitionVersion, DashboardDefinitionVersionExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.DashboardDefinitionVersion, DashboardDefinitionVersionExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string { return Models.DashboardDefinitionVersion.RECORD_IDENTITY; }

    protected getRaw(id: string): Promise<Models.DashboardDefinitionVersion>
    {
        return this.api.dashboardDefinitionVersions.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.DashboardDefinitionVersion[]>
    {
        return this.api.dashboardDefinitionVersions.getBatch(ids);
    }

    async makeHead(id: string): Promise<DashboardDefinitionVersionExtended>
    {
        let model = await this.api.dashboardDefinitionVersions.makeHead(id);
        return this.wrapModel(model);
    }

    async makeRelease(id: string): Promise<DashboardDefinitionVersionExtended>
    {
        let model = await this.api.dashboardDefinitionVersions.makeRelease(id);
        return this.wrapModel(model);
    }
}

export class DashboardDefinitionVersionExtended extends SharedSvc.ExtendedModel<Models.DashboardDefinitionVersion>
{
    static newInstance(svc: DashboardDefinitionVersionsService,
                       model: Models.DashboardDefinitionVersion): DashboardDefinitionVersionExtended
    {
        return new DashboardDefinitionVersionExtended(svc, model, Models.DashboardDefinitionVersion.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.DashboardDefinitionVersion.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    async save(): Promise<DashboardDefinitionVersionExtended>
    {
        let newModel = await this.domain.apis.dashboardDefinitionVersions.create(this.model);
        let verExt   = this.domain.dashboardDefinitionVersions.wrapModel(newModel);

        // Flush the definition, since its head has been changed by the backend.
        let defExt = await verExt.getDefinition();
        await this.domain.dashboardDefinitions.flush(defExt);

        return verExt;
    }

    /**
     * Sets the head to this version. Used for tracking the current edit position.
     * @returns {Promise<DashboardDefinitionVersionExtended>}
     */
    async makeHead(): Promise<DashboardDefinitionVersionExtended>
    {
        return await this.domain.dashboardDefinitionVersions.makeHead(this.model.sysId);
    }

    /**
     * Sets the version to the release version. Scheduled dashboards will use this version.
     * @returns {Promise<DashboardDefinitionVersionExtended>}
     */
    async makeRelease(): Promise<DashboardDefinitionVersionExtended>
    {
        return await this.domain.dashboardDefinitionVersions.makeRelease(this.model.sysId);
    }

    /**
     * Get a new version from the current version.
     * @returns {DashboardDefinitionVersionExtended}
     */
    getNewVersion(): DashboardDefinitionVersionExtended
    {
        let newVersion               = this.domain.dashboardDefinitionVersions.wrapModel(this.model);
        newVersion.model.predecessor = this.getIdentity();
        newVersion.model.sysId       = null;
        return newVersion;
    }

    /**
     * Get stored dashboard details
     */
    async getDetails(): Promise<Models.DashboardConfiguration>
    {
        if (!this.model.details)
        {
            let ext            = await this.domain.dashboardDefinitionVersions.getExtendedByIdentity(this.getIdentity());
            this.model.details = ext.model.details;
        }

        return this.model.details;
    }

    /**
     * Get the parent definition
     */
    getDefinition(): Promise<DashboardDefinitionExtended>
    {
        return this.domain.dashboardDefinitions.getExtendedByIdentity(this.model.definition);
    }

    /**
     * Get the predecessor of this version
     * @returns {Promise<DashboardDefinitionVersionExtended>}
     */
    async getPredecessor(): Promise<DashboardDefinitionVersionExtended>
    {
        return this.domain.dashboardDefinitionVersions.getExtendedByIdentity(this.model.predecessor);
    }
}
