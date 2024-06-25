import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {HostExtended} from "app/services/domain/hosts.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class RepositoryCheckoutsService extends SharedSvc.BaseService<Models.RepositoryCheckout, RepositoryCheckoutExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.RepositoryCheckout, RepositoryCheckoutExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.RepositoryCheckout.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.RepositoryCheckout>
    {
        return this.api.repositoryCheckouts.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.RepositoryCheckout[]>
    {
        return this.api.repositoryCheckouts.getBatch(ids);
    }

    //--//

    @ReportError
    public getList(repo: Models.Repository): Promise<Models.RecordIdentity[]>
    {
        return this.api.repositoryCheckouts.getAll(repo.sysId);
    }

    //--//

    async getExtendedAll(repo: Models.Repository): Promise<RepositoryCheckoutExtended[]>
    {
        let ids = await this.getList(repo);
        return this.getExtendedBatch(ids);
    }
}

export class RepositoryCheckoutExtended extends SharedSvc.ExtendedModel<Models.RepositoryCheckout>
{
    static newInstance(svc: RepositoryCheckoutsService,
                       model: Models.RepositoryCheckout): RepositoryCheckoutExtended
    {
        return new RepositoryCheckoutExtended(svc, model, Models.RepositoryCheckout.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.RepositoryCheckout.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    @Memoizer
    public getOwningHost(): Promise<HostExtended>
    {
        return this.domain.hosts.getExtendedByIdentity(this.model.owningHost);
    }

    //--//

    async checkRemove(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.repositoryCheckouts.remove(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        await this.flush();
        return this.domain.apis.repositoryCheckouts.remove(this.model.sysId);
    }
}

export type RepositoryCheckoutChangeSubscription = SharedSvc.DbChangeSubscription<Models.RepositoryCheckout>;
