import {Injectable} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {ReportDefinitionDetailsExtended, ReportDefinitionExtended} from "app/services/domain/report-definitions.service";

import * as Models from "app/services/proxy/model/models";
import {CacheService} from "framework/services/cache.service";

import {ErrorService} from "framework/services/error.service";

@Injectable()
export class ReportDefinitionVersionsService extends SharedSvc.BaseService<Models.ReportDefinitionVersion, ReportDefinitionVersionExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.ReportDefinitionVersion, ReportDefinitionVersionExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string { return Models.ReportDefinitionVersion.RECORD_IDENTITY; }

    protected getRaw(id: string): Promise<Models.ReportDefinitionVersion>
    {
        return this.api.reportDefinitionVersions.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.ReportDefinitionVersion[]>
    {
        return this.api.reportDefinitionVersions.getBatch(ids);
    }

    async makeHead(id: string): Promise<ReportDefinitionVersionExtended>
    {
        let model = await this.api.reportDefinitionVersions.makeHead(id);
        return this.wrapModel(model);
    }

    async makeRelease(id: string): Promise<ReportDefinitionVersionExtended>
    {
        let model = await this.api.reportDefinitionVersions.makeRelease(id);
        return this.wrapModel(model);
    }

    async link(predId: string,
               succId: string): Promise<ReportDefinitionVersionExtended>
    {
        let model = await this.api.reportDefinitionVersions.link(predId, succId);
        return this.wrapModel(model);
    }
}

export class ReportDefinitionVersionExtended extends SharedSvc.ExtendedModel<Models.ReportDefinitionVersion>
{
    static newInstance(svc: ReportDefinitionVersionsService,
                       model: Models.ReportDefinitionVersion): ReportDefinitionVersionExtended
    {
        return new ReportDefinitionVersionExtended(svc, model, Models.ReportDefinitionVersion.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.ReportDefinitionVersion.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    async save(): Promise<ReportDefinitionVersionExtended>
    {
        let newModel = await this.domain.apis.reportDefinitionVersions.create(this.model);
        let verExt   = this.domain.reportDefinitionVersions.wrapModel(newModel);

        // Flush the definition, since its head has been changed by the backend.
        let defExt = await verExt.getDefinition();
        await this.domain.reportDefinitions.flush(defExt);

        return verExt;
    }

    /**
     * Sets the head to this version. Used for tracking the current edit position.
     * @returns {Promise<ReportDefinitionVersionExtended>}
     */
    async makeHead(): Promise<ReportDefinitionVersionExtended>
    {
        return await this.domain.reportDefinitionVersions.makeHead(this.model.sysId);
    }

    /**
     * Sets the version to the release version. Scheduled reports will use this version.
     * @returns {Promise<ReportDefinitionVersionExtended>}
     */
    async makeRelease(): Promise<ReportDefinitionVersionExtended>
    {
        return await this.domain.reportDefinitionVersions.makeRelease(this.model.sysId);
    }

    /**
     * Will set the head to the predecessor of the current version and link the two. Returns the new head.
     * @returns {Promise<ReportDefinitionVersionExtended>}
     */
    async undo(): Promise<ReportDefinitionVersionExtended>
    {
        let predecessor = await this.getPredecessor();
        if (predecessor)
        {
            predecessor = await predecessor.makeHead();
        }

        return predecessor;
    }

    /**
     * Will set the head to the successor of the current version. Returns the new head.
     *
     * @returns {Promise<ReportDefinitionVersionExtended>}
     */
    async redo(): Promise<ReportDefinitionVersionExtended>
    {
        for (let successor of await this.getSuccessors())
        {
            return successor.makeHead();
        }

        return null;
    }

    /**
     * Link two versions together.
     * @param {ReportDefinitionVersionExtended} succ
     * @returns {Promise<ReportDefinitionVersionExtended>}
     */
    link(succ: ReportDefinitionVersionExtended): Promise<ReportDefinitionVersionExtended>
    {
        return this.domain.reportDefinitionVersions.link(this.model.sysId, succ.model.sysId);
    }

    /**
     * Get a new version from the current version.
     * @returns {ReportDefinitionVersionExtended}
     */
    getNewVersion(): ReportDefinitionVersionExtended
    {
        let newVersion               = this.domain.reportDefinitionVersions.wrapModel(this.model);
        newVersion.model.predecessor = this.getIdentity();
        newVersion.model.sysId       = null;
        return newVersion;
    }

    /**
     * Get stored report details
     */
    getDetails(): Models.ReportDefinitionDetails
    {
        return this.model.details;
    }

    /**
     * Get stored report details extended model
     */
    getDetailsExtended(): ReportDefinitionDetailsExtended
    {
        return new ReportDefinitionDetailsExtended(this.domain, this.model.details);
    }

    /**
     * Get the parent definition
     */
    getDefinition(): Promise<ReportDefinitionExtended>
    {
        return this.domain.reportDefinitions.getExtendedByIdentity(this.model.definition);
    }

    /**
     * Get the predecessor of this version
     * @returns {Promise<ReportDefinitionVersionExtended>}
     */
    async getPredecessor(): Promise<ReportDefinitionVersionExtended>
    {
        return this.domain.reportDefinitionVersions.getExtendedByIdentity(this.model.predecessor);
    }

    /**
     * Get the successor of this version if any
     * @returns {Promise<ReportDefinitionVersionExtended>}
     */
    async getSuccessors(): Promise<ReportDefinitionVersionExtended[]>
    {
        return this.domain.reportDefinitionVersions.getExtendedBatch(this.model.successors);
    }

    async triggerReport(rangeStart: Date,
                        rangeEnd: Date)
    {
        let recReport = Models.Report.newInstance({
                                                      reportDefinitionVersion: this.getIdentity(),
                                                      reportDefinition       : this.model.definition,
                                                      rangeStart             : rangeStart,
                                                      rangeEnd               : rangeEnd
                                                  });

        let report = this.domain.reports.wrapModel(recReport);
        return report.save();
    }
}

