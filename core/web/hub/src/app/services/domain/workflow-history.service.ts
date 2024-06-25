import {Injectable} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";

import * as SharedSvc from "app/services/domain/base.service";
import {WorkflowDetails, WorkflowExtended} from "app/services/domain/events.service";
import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class WorkflowsHistoryService extends SharedSvc.BaseService<Models.WorkflowHistory, WorkflowHistoryExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.WorkflowHistory, WorkflowHistoryExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string { return Models.AlertHistory.RECORD_IDENTITY; }

    protected getRaw(id: string): Promise<Models.WorkflowHistory>
    {
        return this.api.workflows.getWorkflowHistoryByID(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.WorkflowHistory[]>
    {
        return this.api.workflows.getWorkflowHistoryBatch(ids);
    }
}

export class WorkflowHistoryExtended extends SharedSvc.ExtendedModel<Models.WorkflowHistory>
{
    static newInstance(svc: WorkflowsHistoryService,
                       model: Models.WorkflowHistory): WorkflowHistoryExtended
    {
        return new WorkflowHistoryExtended(svc, model, Models.WorkflowHistory.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.WorkflowHistory.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    async getAll()
    {
        let alert = await this.getWorkflow();
        await alert.getAll();
    }

    //--//

    @Memoizer
    public async getWorkflow(): Promise<WorkflowExtended>
    {
        return this.domain.events.getTypedExtendedByIdentity(WorkflowExtended, this.model.workflow);
    }

    public async extractDetails(): Promise<WorkflowDetails>
    {
        let alert   = await this.getWorkflow();
        let details = await alert.extractDetails();

        details.historySysId   = this.model.sysId;
        details.eventTimestamp = MomentHelper.parse(this.model.createdOn);
        details.eventType      = this.model.type;
        details.eventText      = this.model.text;

        return details;
    }
}
