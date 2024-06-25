import {Injectable} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {HostExtended} from "app/services/domain/hosts.service";
import {JobExtended} from "app/services/domain/jobs.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {Memoizer} from "framework/utils/memoizers";


@Injectable()
export class JobStepsService extends SharedSvc.BaseService<Models.JobStep, JobStepExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.JobStep, JobStepExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.JobStep.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.JobStep>
    {
        return this.api.jobSteps.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.JobStep[]>
    {
        return this.api.jobSteps.getBatch(ids);
    }
}

export class JobStepExtended extends SharedSvc.ExtendedModel<Models.JobStep>
{
    static newInstance(svc: JobStepsService,
                       model: Models.JobStep): JobStepExtended
    {
        return new JobStepExtended(svc, model, Models.JobStep.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.JobStep.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    @Memoizer
    public getOwingJob(): Promise<JobExtended>
    {
        return this.domain.jobs.getExtendedByIdentity(this.model.owningJob);
    }

    @Memoizer
    public getBoundHost(): Promise<HostExtended>
    {
        return this.domain.hosts.getExtendedByIdentity(this.model.boundHost);
    }

    filterLog(filters: Models.LogEntryFilterRequest): Promise<Models.LogRange[]>
    {
        return this.domain.apis.jobSteps.filterLog(this.model.sysId, filters);
    }

    getLog(fromOffset?: number,
           toOffset?: number,
           limit?: number): Promise<Models.LogLine[]>
    {
        return this.domain.apis.jobSteps.getLog(this.model.sysId, fromOffset, toOffset, limit);
    }
}

export type JobStepChangeSubscription = SharedSvc.DbChangeSubscription<Models.JobStep>;
