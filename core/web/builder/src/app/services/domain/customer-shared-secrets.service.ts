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
export class CustomerSharedSecretsService extends SharedSvc.BaseService<Models.CustomerSharedSecret, CustomerSharedSecretExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService,
                private enums: EnumsService)
    {
        super(api, errors, cache, Models.CustomerSharedSecret, CustomerSharedSecretExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.CustomerSharedSecret.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.CustomerSharedSecret>
    {
        return this.api.customerSharedSecrets.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.CustomerSharedSecret[]>
    {
        return this.api.customerSharedSecrets.getBatch(ids);
    }

    //--//

    async save(extended: CustomerSharedSecretExtended): Promise<CustomerSharedSecretExtended>
    {
        this.flush(extended);

        await this.api.customerSharedSecrets.update(extended.model.sysId, undefined, extended.model);

        return this.refreshExtended(extended);
    }
}

export class CustomerSharedSecretExtended extends SharedSvc.ExtendedModel<Models.CustomerSharedSecret>
{
    static newInstance(svc: CustomerSharedSecretsService,
                       model: Models.CustomerSharedSecret): CustomerSharedSecretExtended
    {
        return new CustomerSharedSecretExtended(svc, model, Models.CustomerSharedSecret.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.CustomerSharedSecret.RECORD_IDENTITY,
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
    async save(): Promise<CustomerSharedSecretExtended>
    {
        return this.domain.customerSharedSecrets.save(this);
    }

    async checkRemove(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.customerSharedSecrets.remove(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        await this.flush();
        return this.domain.apis.customerSharedSecrets.remove(this.model.sysId);
    }

    //--//

    static parseRoles(input: string): string[]
    {
        return input ? input.split("/")
                            .map((role) => role.trim())
                            .filter((v) => v && v.length > 0) : ["SYS.USER"];
    }
}

export type CustomerSharedSecretChangeSubscription = SharedSvc.DbChangeSubscription<Models.CustomerSharedSecret>;
