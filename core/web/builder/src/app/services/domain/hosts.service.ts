import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {RepositoryCheckoutExtended} from "app/services/domain/repository-checkouts.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {Memoizer} from "framework/utils/memoizers";


@Injectable()
export class HostsService extends SharedSvc.BaseService<Models.Host, HostExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.Host, HostExtended.newInstance);
    }

    /**
     * Get the list of hosts.
     */
    @ReportError
    public getList(): Promise<Array<Models.RecordIdentity>>
    {
        return this.api.hosts.getAll();
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.Host.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.Host>
    {
        return this.api.hosts.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.Host[]>
    {
        return this.api.hosts.getBatch(ids);
    }

    //--//

    /**
     * Get all device and associated data.
     */
    async getExtendedAll(): Promise<HostExtended[]>
    {
        let ids = await this.getList();
        return this.getExtendedBatch(ids);
    }
}

export class HostExtended extends SharedSvc.ExtendedModel<Models.Host>
{
    static newInstance(svc: HostsService,
                       model: Models.Host): HostExtended
    {
        return new HostExtended(svc, model, Models.Host.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.Host.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    @Memoizer
    public getCheckouts(): Promise<RepositoryCheckoutExtended[]>
    {
        return this.domain.repositoryCheckouts.getExtendedBatch(this.model.checkouts);
    }

    //--//

    filterLog(filters: Models.LogEntryFilterRequest): Promise<Models.LogRange[]>
    {
        return this.domain.apis.hosts.filterLog(this.model.sysId, filters);
    }

    getLog(fromOffset?: number,
           toOffset?: number,
           limit?: number): Promise<Models.LogLine[]>
    {
        return this.domain.apis.hosts.getLog(this.model.sysId, fromOffset, toOffset, limit);
    }

    deleteLog(olderThan?: number): Promise<number>
    {
        return this.domain.apis.hosts.deleteLog(this.model.sysId, olderThan);
    }
}

export type HostChangeSubscription = SharedSvc.DbChangeSubscription<Models.Host>;
