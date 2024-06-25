import {Injectable} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {JobDefinitionExtended} from "app/services/domain/job-definitions.service";
import {RepositoryExtended} from "app/services/domain/repositories.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class JobDefinitionStepsService extends SharedSvc.BaseService<Models.JobDefinitionStep, JobDefinitionStepExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.JobDefinitionStep, JobDefinitionStepExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.JobDefinitionStep.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.JobDefinitionStep>
    {
        return this.api.jobDefinitionSteps.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.JobDefinitionStep[]>
    {
        return this.api.jobDefinitionSteps.getBatch(ids);
    }

    //--//

    wrapModel(model?: Models.JobDefinitionStep): JobDefinitionStepExtended
    {
        let ext = super.wrapModel(model);

        if (model instanceof Models.JobDefinitionStepForDockerBuild)
        {
            return Object.setPrototypeOf(ext, JobDefinitionStepExtendedForDockerBuild.prototype);
        }

        if (model instanceof Models.JobDefinitionStepForDockerPush)
        {
            return Object.setPrototypeOf(ext, JobDefinitionStepExtendedForDockerPush.prototype);
        }

        if (model instanceof Models.JobDefinitionStepForDockerRun)
        {
            return Object.setPrototypeOf(ext, JobDefinitionStepExtendedForDockerRun.prototype);
        }

        if (model instanceof Models.JobDefinitionStepForGit)
        {
            return Object.setPrototypeOf(ext, JobDefinitionStepExtendedForGit.prototype);
        }

        if (model instanceof Models.JobDefinitionStepForMaven)
        {
            return Object.setPrototypeOf(ext, JobDefinitionStepExtendedForMaven.prototype);
        }

        if (model instanceof Models.JobDefinitionStepForSshCommand)
        {
            return Object.setPrototypeOf(ext, JobDefinitionStepExtendedForSshCommand.prototype);
        }

        return ext;
    }
}

export class JobDefinitionStepExtended extends SharedSvc.ExtendedModel<Models.JobDefinitionStep>
{
    static newInstance(svc: JobDefinitionStepsService,
                       model: Models.JobDefinitionStep): JobDefinitionStepExtended
    {
        return new JobDefinitionStepExtended(svc, model, Models.JobDefinitionStep.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.JobDefinitionStep.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get asDockerBuild(): Models.JobDefinitionStepForDockerBuild
    {
        return <Models.JobDefinitionStepForDockerBuild> this.model;
    }

    get asDockerPush(): Models.JobDefinitionStepForDockerPush
    {
        return <Models.JobDefinitionStepForDockerPush> this.model;
    }

    get asDockerRun(): Models.JobDefinitionStepForDockerRun
    {
        return <Models.JobDefinitionStepForDockerRun> this.model;
    }

    get asGit(): Models.JobDefinitionStepForGit
    {
        return <Models.JobDefinitionStepForGit> this.model;
    }

    get asGitExt(): JobDefinitionStepExtendedForGit
    {
        return <JobDefinitionStepExtendedForGit> <any> this;
    }

    get asMaven(): Models.JobDefinitionStepForMaven
    {
        return <Models.JobDefinitionStepForMaven> this.model;
    }

    get asSshCommand(): Models.JobDefinitionStepForSshCommand
    {
        return <Models.JobDefinitionStepForSshCommand> this.model;
    }

    @Memoizer
    public getOwingJob(): Promise<JobDefinitionExtended>
    {
        return this.domain.jobDefinitions.getExtendedByIdentity(this.model.owningJob);
    }
}

export class JobDefinitionStepExtendedForDockerBuild extends JobDefinitionStepExtended
{
}

export class JobDefinitionStepExtendedForDockerPush extends JobDefinitionStepExtended
{
}

export class JobDefinitionStepExtendedForDockerRun extends JobDefinitionStepExtended
{
}

export class JobDefinitionStepExtendedForGit extends JobDefinitionStepExtended
{
    @Memoizer
    public getRepo(): Promise<RepositoryExtended>
    {
        return this.domain.repositories.getExtendedByIdentity(this.asGit.repo);
    }

    @Memoizer
    async getRepoUrl(): Promise<String>
    {
        let repo = await this.getRepo();
        return repo.model.gitUrl;
    }
}

export class JobDefinitionStepExtendedForMaven extends JobDefinitionStepExtended
{
}

export class JobDefinitionStepExtendedForSshCommand extends JobDefinitionStepExtended
{
}

export type JobDefinitionStepChangeSubscription = SharedSvc.DbChangeSubscription<Models.JobDefinitionStep>;
