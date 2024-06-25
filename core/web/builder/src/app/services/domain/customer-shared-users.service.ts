import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {CustomerExtended} from "app/services/domain/customers.service";
import {EnumsService} from "app/services/domain/enums.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class CustomerSharedUsersService extends SharedSvc.BaseService<Models.CustomerSharedUser, CustomerSharedUserExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService,
                private enums: EnumsService)
    {
        super(api, errors, cache, Models.CustomerSharedUser, CustomerSharedUserExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.CustomerSharedUser.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.CustomerSharedUser>
    {
        return this.api.customerSharedUsers.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.CustomerSharedUser[]>
    {
        return this.api.customerSharedUsers.getBatch(ids);
    }

    //--//

    async save(extended: CustomerSharedUserExtended): Promise<CustomerSharedUserExtended>
    {
        this.flush(extended);

        await this.api.customerSharedUsers.update(extended.model.sysId, undefined, extended.model);

        return this.refreshExtended(extended);
    }
}

export class CustomerSharedUserExtended extends SharedSvc.ExtendedModel<Models.CustomerSharedUser>
{
    static newInstance(svc: CustomerSharedUsersService,
                       model: Models.CustomerSharedUser): CustomerSharedUserExtended
    {
        return new CustomerSharedUserExtended(svc, model, Models.CustomerSharedUser.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.CustomerSharedUser.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    @Memoizer
    public getOwningCustomer(): Promise<CustomerExtended>
    {
        return this.domain.customers.getExtendedByIdentity(this.model.customer);
    }

    //--//

    @ReportError
    async save(): Promise<CustomerSharedUserExtended>
    {
        return this.domain.customerSharedUsers.save(this);
    }

    async checkRemove(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.customerSharedUsers.remove(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        await this.flush();
        return this.domain.apis.customerSharedUsers.remove(this.model.sysId);
    }

    //--//

    static parseRoles(input: string): string[]
    {
        return input ? input.split("/")
                            .map((role) => role.trim())
                            .filter((v) => v && v.length > 0) : ["SYS.USER"];
    }
}

export type CustomerSharedUserChangeSubscription = SharedSvc.DbChangeSubscription<Models.CustomerSharedUser>;
