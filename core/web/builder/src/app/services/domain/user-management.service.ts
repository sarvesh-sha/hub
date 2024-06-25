import {Injectable} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {SettingsService} from "app/services/domain/settings.service";

import * as Models from "app/services/proxy/model/models";
import {CacheService} from "framework/services/cache.service";

import {ErrorService} from "framework/services/error.service";

@Injectable()
export class UserManagementService extends SharedSvc.BaseService<Models.User, UserExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService,
                public settings: SettingsService)
    {
        super(api, errors, cache, Models.User, UserExtended.newInstance);
    }

    protected cachePrefix(): string
    {
        return Models.User.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.User>
    {
        return this.settings.getUserByID(id);
    }

    protected async getBatchRaw(ids: string[]): Promise<Models.User[]>
    {
        const users  = await this.settings.getUsersList();
        const lookup = new Map<string, Models.User>();
        for (let user of users)
        {
            lookup.set(user.sysId, user);
        }
        return ids.map((id) => lookup.get(id));
    }
}

export class UserExtended extends SharedSvc.ExtendedModel<Models.User>
{
    static newInstance(svc: UserManagementService,
                       model: Models.User): UserExtended
    {
        return new UserExtended(svc, model, Models.User.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.User.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get fullName(): string
    {
        return `${this.model.firstName} ${this.model.lastName}`;
    }
}
