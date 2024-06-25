import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {JobDefinitionStepExtended} from "app/services/domain/job-definition-steps.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class JobDefinitionsService extends SharedSvc.BaseService<Models.JobDefinition, JobDefinitionExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.JobDefinition, JobDefinitionExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.JobDefinition.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.JobDefinition>
    {
        return this.api.jobDefinitions.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.JobDefinition[]>
    {
        return this.api.jobDefinitions.getBatch(ids);
    }

    //--//

    @ReportError
    public getList(): Promise<Models.RecordIdentity[]>
    {
        return this.api.jobDefinitions.getAll();
    }

    //--//

    async getExtendedAll(): Promise<JobDefinitionExtended[]>
    {
        let ids = await this.getList();
        return this.getExtendedBatch(ids);
    }

    //--//

    @ReportError
    public trigger(id: string,
                   branch: string,
                   commit: string): Promise<Models.Job>
    {
        return this.api.jobDefinitions.trigger(id, branch, commit);
    }
}

export class JobDefinitionExtended extends SharedSvc.ExtendedModel<Models.JobDefinition>
{
    static newInstance(svc: JobDefinitionsService,
                       model: Models.JobDefinition): JobDefinitionExtended
    {
        return new JobDefinitionExtended(svc, model, Models.JobDefinition.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.JobDefinition.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    @Memoizer
    public getSteps(): Promise<Array<JobDefinitionStepExtended>>
    {
        return this.domain.jobDefinitionSteps.getExtendedBatch(this.model.steps);
    }
}

export type JobDefinitionChangeSubscription = SharedSvc.DbChangeSubscription<Models.JobDefinition>;
