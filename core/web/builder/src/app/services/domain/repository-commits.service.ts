import {Injectable} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";

@Injectable()
export class RepositoryCommitsService extends SharedSvc.BaseService<Models.RepositoryCommit, RepositoryCommitExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.RepositoryCommit, RepositoryCommitExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.RepositoryCommit.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.RepositoryCommit>
    {
        return this.api.repositoryCommits.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.RepositoryCommit[]>
    {
        return this.api.repositoryCommits.getBatch(ids);
    }

    //--//

    async getAncestors(commit: RepositoryCommitExtended,
                       depth: number): Promise<RepositoryCommitExtended[]>
    {
        let ids = await this.api.repositoryCommits.getAncestors(commit.model.sysId, depth);
        return this.getExtendedBatch(ids);
    }
}

export class RepositoryCommitExtended extends SharedSvc.ExtendedModel<Models.RepositoryCommit>
{
    static newInstance(svc: RepositoryCommitsService,
                       model: Models.RepositoryCommit): RepositoryCommitExtended
    {
        return new RepositoryCommitExtended(svc, model, Models.RepositoryCommit.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.RepositoryCommit.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    getParents(depth: number): Promise<RepositoryCommitExtended[]>
    {
        return this.domain.repositoryCommits.getAncestors(this, depth);
    }
}
