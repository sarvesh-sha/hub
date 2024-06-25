import {Injectable} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {JobExtended} from "app/services/domain/jobs.service";
import {RepositoryExtended} from "app/services/domain/repositories.service";
import {RepositoryCommitExtended} from "app/services/domain/repository-commits.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {Memoizer} from "framework/utils/memoizers";


@Injectable()
export class JobSourcesService extends SharedSvc.BaseService<Models.JobSource, JobSourceExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.JobSource, JobSourceExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.JobSource.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.JobSource>
    {
        return this.api.jobSources.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.JobSource[]>
    {
        return this.api.jobSources.getBatch(ids);
    }
}

export class JobSourceExtended extends SharedSvc.ExtendedModel<Models.JobSource>
{
    static newInstance(svc: JobSourcesService,
                       model: Models.JobSource): JobSourceExtended
    {
        return new JobSourceExtended(svc, model, Models.JobSource.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.JobSource.RECORD_IDENTITY,
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
    public getRepo(): Promise<RepositoryExtended>
    {
        return this.domain.repositories.getExtendedByIdentity(this.model.repo);
    }

    @Memoizer
    async getCommit(): Promise<RepositoryCommitExtended>
    {
        let repo = await this.getRepo();
        if (!repo) return null;

        let commit = await this.domain.apis.repositories.getCommit(repo.model.sysId, this.model.commit);
        return this.domain.repositoryCommits.wrapModel(commit);
    }
}

export type JobSourceChangeSubscription = SharedSvc.DbChangeSubscription<Models.JobSource>;
