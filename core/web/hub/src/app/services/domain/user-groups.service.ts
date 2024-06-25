import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";

@Injectable()
export class UserGroupsService extends SharedSvc.BaseService<Models.UserGroup, UserGroupExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.UserGroup, UserGroupExtended.newInstance);
    }

    /**
     * Get the list of UserGroups.
     */
    @ReportError
    public getList(): Promise<Models.RecordIdentity[]>
    {
        return this.api.userGroups.getAll();
    }

    //--//

    async getExtendedAll(): Promise<UserGroupExtended[]>
    {
        let ids = await this.getList();
        return this.getExtendedBatch(ids);
    }

    //--//

    protected cachePrefix(): string { return Models.UserGroup.RECORD_IDENTITY; }

    protected getRaw(id: string): Promise<Models.UserGroup>
    {
        return this.api.userGroups.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.UserGroup[]>
    {
        return this.api.userGroups.getBatch(ids);
    }

    //--//


    /**
     * Create a new user group
     */
    @ReportError
    async createUserGroup(userGroup: Models.UserGroupCreationRequest): Promise<Models.UserGroup>
    {
        return await this.api.userGroups.create(userGroup);
    }

    /**
     * Update the user group
     */
    @ReportError
    async saveUserGroup(userGroup: Models.UserGroup): Promise<Models.UserGroup>
    {
        await this.api.userGroups.update(userGroup.sysId, undefined, userGroup);

        return this.api.userGroups.get(userGroup.sysId);
    }

    /**
     * Delete a user group.
     */
    @ReportError
    async deleteUserGroup(userGroup: Models.UserGroup): Promise<Models.ValidationResults>
    {
        return await this.api.userGroups.remove(userGroup.sysId);
    }
}

export class UserGroupExtended extends SharedSvc.ExtendedModel<Models.UserGroup>
{
    static newInstance(svc: UserGroupsService,
                       model: Models.UserGroup): UserGroupExtended
    {
        return new UserGroupExtended(svc, model, Models.UserGroup.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.UserGroup.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }
}
