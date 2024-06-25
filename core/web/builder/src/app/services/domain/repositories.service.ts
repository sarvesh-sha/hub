import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {RepositoryBranchExtended} from "app/services/domain/repository-branches.service";
import {RepositoryCheckoutExtended} from "app/services/domain/repository-checkouts.service";
import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {Future} from "framework/utils/concurrency";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class RepositoriesService extends SharedSvc.BaseService<Models.Repository, RepositoryExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.Repository, RepositoryExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.Repository.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.Repository>
    {
        return this.api.repositories.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.Repository[]>
    {
        return this.api.repositories.getBatch(ids);
    }

    //--//

    @ReportError
    public getList(): Promise<Models.RecordIdentity[]>
    {
        return this.api.repositories.getAll();
    }

    //--//

    async getExtendedAll(): Promise<RepositoryExtended[]>
    {
        let ids = await this.getList();
        return this.getExtendedBatch(ids);
    }

    async refresh(): Promise<Models.RepositoryRefresh>
    {
        let sysId = await this.api.repositories.startRefresh();
        while (true)
        {
            let res = await this.api.repositories.checkRefresh(sysId, true);
            if (res.status == Models.BackgroundActivityStatus.COMPLETED)
            {
                return res;
            }

            await Future.delayed(100);
        }
    }
}

export class RepositoryExtended extends SharedSvc.ExtendedModel<Models.Repository>
{
    static newInstance(svc: RepositoriesService,
                       model: Models.Repository): RepositoryExtended
    {
        return new RepositoryExtended(svc, model, Models.Repository.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.Repository.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    @Memoizer
    public getCheckouts(): Promise<RepositoryCheckoutExtended[]>
    {
        return this.domain.repositoryCheckouts.getExtendedBatch(this.model.checkouts);
    }

    public getBranches(): Promise<RepositoryBranchExtended[]>
    {
        return this.domain.repositoryBranches.getExtendedBatch(this.model.branches);
    }

    //--//

    async checkRemove(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.repositories.remove(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        await this.flush();
        return this.domain.apis.repositories.remove(this.model.sysId);
    }
}

export type RepositoryChangeSubscription = SharedSvc.DbChangeSubscription<Models.Repository>;
