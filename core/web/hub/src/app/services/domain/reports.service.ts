import {Injectable} from "@angular/core";
import {ReportError} from "app/app.service";

import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {ReportDefinitionVersionExtended} from "app/services/domain/report-definition-versions.service";
import {ReportDefinitionExtended} from "app/services/domain/report-definitions.service";

import * as Models from "app/services/proxy/model/models";
import {CacheService} from "framework/services/cache.service";

import {ErrorService} from "framework/services/error.service";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class ReportsService extends SharedSvc.BaseService<Models.Report, ReportExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.Report, ReportExtended.newInstance);
    }

    @ReportError
    public getAll(): Promise<Models.RecordIdentity[]>
    {
        return this.api.reports.getAll();
    }

    @ReportError
    async getExtendedAll(): Promise<ReportExtended[]>
    {
        let ids = await this.getAll();
        let all = await this.getExtendedBatch(ids);
        return all;
    }

    /**
     * Get the list of devices, applying optional filters.
     */
    @ReportError
    public getList(filters: Models.ReportFilterRequest): Promise<Models.RecordIdentity[]>
    {
        return this.api.reports.getFiltered(filters);
    }

    //--//

    protected cachePrefix(): string { return Models.Report.RECORD_IDENTITY; }

    protected getRaw(id: string): Promise<Models.Report>
    {
        return this.api.reports.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.Report[]>
    {
        return this.api.reports.getBatch(ids);
    }
}

export class ReportExtended extends SharedSvc.ExtendedModel<Models.Report>
{
    static newInstance(svc: ReportsService,
                       model: Models.Report): ReportExtended
    {
        return new ReportExtended(svc, model, Models.Report.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.Report.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get isFinished()
    {
        if (this.model && this.model.status == Models.ReportStatus.Finished) return true;
        return false;
    }

    get isFailed()
    {
        if (this.model && this.model.status == Models.ReportStatus.Failed) return true;
        return false;
    }

    retry(): Promise<Models.Report>
    {
        this.model.status = Models.ReportStatus.Queued;
        return this.domain.apis.reports.retry(this.model.sysId);
    }

    @Memoizer
    async getReportDefinitionVersion()
    {
        return this.domain.reportDefinitionVersions.getExtendedByIdentity(this.model.reportDefinitionVersion);
    }

    async save(): Promise<ReportExtended>
    {
        if (this.model.sysId)
        {
            throw new Error("Updates not allowed.");
        }
        else
        {
            let newModel = await this.domain.apis.reports.create(this.model);
            return this.domain.reports.wrapModel(newModel);
        }
    }

    getDownloadUrl(name: string): string
    {
        let encodedName = encodeURIComponent(name);
        let timestamp   = MomentHelper.fileNameFormat(MomentHelper.parse(this.model.createdOn));

        return this.domain.apis.reports.downloadReport__generateUrl(this.model.sysId, `${encodedName}__${timestamp}.pdf`);
    }

    getDefinition(): Promise<ReportDefinitionExtended>
    {
        return this.domain.reportDefinitions.getExtendedByIdentity(this.model.reportDefinition);
    }

    getDefinitionVersion(): Promise<ReportDefinitionVersionExtended>
    {
        return this.domain.reportDefinitionVersions.getExtendedByIdentity(this.model.reportDefinitionVersion);
    }
}
