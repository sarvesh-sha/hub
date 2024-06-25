import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";
import {CustomerExtended} from "app/services/domain/customers.service";
import {EnumsService} from "app/services/domain/enums.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class CustomerServiceSecretsService extends SharedSvc.BaseService<Models.CustomerServiceSecret, CustomerServiceSecretExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService,
                private enums: EnumsService)
    {
        super(api, errors, cache, Models.CustomerServiceSecret, CustomerServiceSecretExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.CustomerServiceSecret.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.CustomerServiceSecret>
    {
        return this.api.customerServiceSecrets.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.CustomerServiceSecret[]>
    {
        return this.api.customerServiceSecrets.getBatch(ids);
    }

    //--//

    async save(extended: CustomerServiceSecretExtended): Promise<CustomerServiceSecretExtended>
    {
        this.flush(extended);

        await this.api.customerServiceSecrets.update(extended.model.sysId, undefined, extended.model);

        return this.refreshExtended(extended);
    }
}

export class CustomerServiceSecretExtended extends SharedSvc.ExtendedModel<Models.CustomerServiceSecret>
{
    static newInstance(svc: CustomerServiceSecretsService,
                       model: Models.CustomerServiceSecret): CustomerServiceSecretExtended
    {
        return new CustomerServiceSecretExtended(svc, model, Models.CustomerServiceSecret.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.CustomerServiceSecret.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    @Memoizer
    public getOwningService(): Promise<CustomerServiceExtended>
    {
        return this.domain.customerServices.getExtendedByIdentity(this.model.service);
    }

    //--//

    @ReportError
    async save(): Promise<CustomerServiceSecretExtended>
    {
        return this.domain.customerServiceSecrets.save(this);
    }

    async checkRemove(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.customerServiceSecrets.remove(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        await this.flush();
        return this.domain.apis.customerServiceSecrets.remove(this.model.sysId);
    }

    //--//

    static parseRoles(input: string): string[]
    {
        return input ? input.split("/")
                            .map((role) => role.trim())
                            .filter((v) => v && v.length > 0) : ["SYS.USER"];
    }
}

export type CustomerServiceSecretChangeSubscription = SharedSvc.DbChangeSubscription<Models.CustomerServiceSecret>;
