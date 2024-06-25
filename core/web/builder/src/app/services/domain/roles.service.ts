import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";

@Injectable()
export class RolesService extends SharedSvc.BaseService<Models.Role, RoleExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.Role, RoleExtended.newInstance);
    }

    /**
     * Get the list of roles.
     */
    @ReportError
    public getList(): Promise<Models.RecordIdentity[]>
    {
        return this.api.roles.getAll();
    }

    //--//

    async getExtendedAll(): Promise<RoleExtended[]>
    {
        let ids = await this.getList();
        return this.getExtendedBatch(ids);
    }

    //--//

    protected cachePrefix(): string { return Models.Role.RECORD_IDENTITY; }

    protected getRaw(id: string): Promise<Models.Role>
    {
        return this.api.roles.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.Role[]>
    {
        return this.api.roles.getBatch(ids);
    }
}

export class RoleExtended extends SharedSvc.ExtendedModel<Models.Role>
{
    static newInstance(svc: RolesService,
                       model: Models.Role): RoleExtended
    {
        return new RoleExtended(svc, model, Models.Role.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.Role.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }
}
