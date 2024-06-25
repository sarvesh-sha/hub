import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {JobSourceExtended} from "app/services/domain/job-sources.service";
import {JobStepExtended} from "app/services/domain/job-steps.service";
import {RegistryTaggedImageExtended} from "app/services/domain/registry-tagged-images.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {inParallel} from "framework/utils/concurrency";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class JobsService extends SharedSvc.BaseService<Models.Job, JobExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.Job, JobExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.Job.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.Job>
    {
        return this.api.jobs.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.Job[]>
    {
        return this.api.jobs.getBatch(ids);
    }

    //--//

    @ReportError
    public getList(): Promise<Models.RecordIdentity[]>
    {
        return this.api.jobs.getAll();
    }

    //--//

    async getExtendedAll(): Promise<JobExtended[]>
    {
        let ids = await this.getList();
        return this.getExtendedBatch(ids);
    }

    //--//

    public async computeJobUsage(): Promise<Models.JobUsage[]>
    {
        let results: Models.JobUsage[] = [];

        let jobs = await this.getExtendedAll();
        await inParallel(jobs,
                         async (job,
                                index) =>
                         {
                             let jobUsage = await job.getUsage();
                             results.push(jobUsage);
                         });

        results.sort((a,
                      b) => MomentHelper.compareDates(b.createdOn, a.createdOn)); // From newest to oldest

        return results;
    }
}

export class JobExtended extends SharedSvc.ExtendedModel<Models.Job>
{
    static newInstance(svc: JobsService,
                       model: Models.Job): JobExtended
    {
        return new JobExtended(svc, model, Models.Job.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.Job.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    @Memoizer
    public getSources(): Promise<JobSourceExtended[]>
    {
        return this.domain.jobSources.getExtendedBatch(this.model.sources);
    }

    @Memoizer
    public getSteps(): Promise<JobStepExtended[]>
    {
        return this.domain.jobSteps.getExtendedBatch(this.model.steps);
    }

    @Memoizer
    public getImages(): Promise<RegistryTaggedImageExtended[]>
    {
        return this.domain.registryTaggedImages.getExtendedBatch(this.model.generatedImages);
    }

    @Memoizer
    public getUsage(): Promise<Models.JobUsage>
    {
        return this.domain.apis.jobs.getUsage(this.model.sysId);
    }

    //--//

    async cancel(): Promise<JobExtended>
    {
        let job = await this.domain.apis.jobs.cancel(this.model.sysId);
        return this.domain.jobs.wrapModel(job);
    }

    async checkRemove(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.jobs.remove(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        await this.flush();
        return this.domain.apis.jobs.remove(this.model.sysId);
    }
}

export type JobChangeSubscription = SharedSvc.DbChangeSubscription<Models.Job>;
