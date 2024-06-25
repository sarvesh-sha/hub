import {Injectable} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {RepositoryExtended} from "app/services/domain/repositories.service";
import {RepositoryCommitExtended} from "app/services/domain/repository-commits.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class RepositoryBranchesService extends SharedSvc.BaseService<Models.RepositoryBranch, RepositoryBranchExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.RepositoryBranch, RepositoryBranchExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.RepositoryBranch.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.RepositoryBranch>
    {
        return this.api.repositoryBranches.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.RepositoryBranch[]>
    {
        return this.api.repositoryBranches.getBatch(ids);
    }
}

export class RepositoryBranchExtended extends SharedSvc.ExtendedModel<Models.RepositoryBranch>
{
    static newInstance(svc: RepositoryBranchesService,
                       model: Models.RepositoryBranch): RepositoryBranchExtended
    {
        return new RepositoryBranchExtended(svc, model, Models.RepositoryBranch.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.RepositoryBranch.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    @Memoizer
    public getRepo(): Promise<RepositoryExtended>
    {
        return this.domain.repositories.getExtendedByIdentity(this.model.repository);
    }

    @Memoizer
    public getHead(): Promise<RepositoryCommitExtended>
    {
        return this.domain.repositoryCommits.getExtendedByIdentity(this.model.head);
    }
}
