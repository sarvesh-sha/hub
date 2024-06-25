import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class CustomersService extends SharedSvc.BaseService<Models.Customer, CustomerExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.Customer, CustomerExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.Customer.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.Customer>
    {
        return this.api.customers.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.Customer[]>
    {
        return this.api.customers.getBatch(ids);
    }

    //--//

    @ReportError
    public getList(): Promise<Models.RecordIdentity[]>
    {
        return this.api.customers.getAll();
    }

    //--//

    async getExtendedAll(): Promise<CustomerExtended[]>
    {
        let ids = await this.getList();
        return this.getExtendedBatch(ids);
    }

    async save(extended: CustomerExtended): Promise<CustomerExtended>
    {
        if (extended.model.sysId)
        {
            this.flush(extended);

            await this.api.customers.update(extended.model.sysId, undefined, extended.model);

            return this.refreshExtended(extended);
        }
        else
        {
            let newModel = await this.api.customers.create(extended.model);
            return this.wrapModel(newModel);
        }
    }
}

export class CustomerExtended extends SharedSvc.ExtendedModel<Models.Customer>
{
    static newInstance(svc: CustomersService,
                       model: Models.Customer): CustomerExtended
    {
        return new CustomerExtended(svc, model, Models.Customer.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.Customer.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    @Memoizer
    public getServices(): Promise<CustomerServiceExtended[]>
    {
        return this.domain.customerServices.getExtendedBatch(this.model.services);
    }

    //--//

    async checkRemove(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.customers.remove(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        await this.flush();
        return this.domain.apis.customers.remove(this.model.sysId);
    }

    @ReportError
    async createUser(userRequest: Models.UserCreationRequest): Promise<Models.CustomerSharedUser>
    {
        return this.domain.apis.customerSharedUsers.create(this.model.sysId, userRequest);
    }

    @ReportError
    async createSecret(model: Models.CustomerSharedSecret): Promise<Models.CustomerSharedSecret>
    {
        return this.domain.apis.customerSharedSecrets.create(this.model.sysId, model);
    }

    //--//

    getCharges(maxTopHosts: number): Promise<Models.DeploymentCellularChargesSummary>
    {
        return this.domain.apis.customers.getCharges(this.model.sysId, maxTopHosts);
    }
}

export type CustomerChangeSubscription = SharedSvc.DbChangeSubscription<Models.Customer>;
