import {Injectable} from "@angular/core";

import {AppDomainContext} from "app/services/domain";
import * as SharedSvc from "app/services/domain/base.service";
import {BaseModel, ExtendedModel} from "app/services/domain/base.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";
import {CustomerExtended} from "app/services/domain/customers.service";
import {DeploymentAgentExtended} from "app/services/domain/deployment-agents.service";
import {DeploymentHostExtended} from "app/services/domain/deployment-hosts.service";
import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {UtilsService} from "framework/services/utils.service";

@Injectable()
export class DashboardManagementService
{
    public static readonly NavigationHistoryKeyName: string = "NAVIGATION_HISTORY";

    private m_state: NavigationHistory;
    private m_saveTimer: any;

    constructor(private domain: AppDomainContext,
                private cache: CacheService,
                private utils: UtilsService)
    {
    }

    public async getLastCustomers(): Promise<CustomerExtended[]>
    {
        let state = await this.getHistory();

        let res = await state.toObjects(this.domain.customers, state.customers, (sysId) => CustomerExtended.newIdentity(sysId));
        if (state.modified)
        {
            this.flushHistory();
        }

        return res;
    }

    public async getLastServices(): Promise<CustomerServiceExtended[]>
    {
        let state = await this.getHistory();

        let res = await state.toObjects(this.domain.customerServices, state.services, (sysId) => CustomerServiceExtended.newIdentity(sysId));
        if (state.modified)
        {
            this.flushHistory();
        }

        return res;
    }

    public async getLastHosts(): Promise<DeploymentHostExtended[]>
    {
        let state = await this.getHistory();

        let res = await state.toObjects(this.domain.deploymentHosts, state.hosts, (sysId) => DeploymentHostExtended.newIdentity(sysId));
        if (state.modified)
        {
            this.flushHistory();
        }

        return res;
    }

    public async getLastAgents(): Promise<DeploymentAgentExtended[]>
    {
        let state = await this.getHistory();

        let res = await state.toObjects(this.domain.deploymentAgents, state.agents, (sysId) => DeploymentAgentExtended.newIdentity(sysId));
        if (state.modified)
        {
            this.flushHistory();
        }

        return res;
    }

    //--//

    public async recordCustomer(cust: CustomerExtended)
    {
        let state = await this.getHistory();
        state.recordCustomer(cust);

        this.flushHistory();
    }

    public async recordService(svc: CustomerServiceExtended)
    {
        let state = await this.getHistory();
        state.recordService(svc);

        let cust = await svc.getOwningCustomer();
        if (cust)
        {
            state.recordCustomer(cust);
        }

        this.flushHistory();
    }

    public async recordHost(host: DeploymentHostExtended)
    {
        let state = await this.getHistory();
        state.recordHost(host);

        let svc = await host.getCustomerService();
        if (svc)
        {
            state.recordService(svc);

            let cust = await svc.getOwningCustomer();
            if (cust)
            {
                state.recordCustomer(cust);
            }
        }

        this.flushHistory();
    }

    public async recordAgent(agent: DeploymentAgentExtended)
    {
        let state = await this.getHistory();
        state.recordAgent(agent);

        let host = await agent.getOwningDeployment();
        if (host)
        {
            state.recordHost(host);

            let svc = await host.getCustomerService();
            if (svc)
            {
                state.recordService(svc);

                let cust = await svc.getOwningCustomer();
                if (cust)
                {
                    state.recordCustomer(cust);
                }
            }
        }

        this.flushHistory();
    }

    //--//

    public async getHistory(): Promise<NavigationHistory>
    {
        if (!this.m_state)
        {
            this.m_state = await this.domain.users.getTypedPreference<NavigationHistory>(null, DashboardManagementService.NavigationHistoryKeyName, DashboardManagementService.fixupPrototype);
        }

        if (!this.m_state)
        {
            this.m_state = new NavigationHistory();
        }

        return this.m_state;
    }

    private async flushHistory()
    {
        if (this.m_state)
        {
            await this.cache.set(DashboardManagementService.NavigationHistoryKeyName, this.m_state);
        }

        this.saveHistoryDelayed();
    }

    private async saveHistoryDelayed()
    {
        if (!this.m_saveTimer)
        {
            this.m_saveTimer = this.utils.setTimeoutOutsideAngular(async () =>
                                                                   {
                                                                       await this.saveHistoryNow();

                                                                       this.m_saveTimer = null;
                                                                   }, 30 * 1000);
        }
    }

    private async saveHistoryNow()
    {
        if (this.m_state && this.m_state.modified)
        {
            this.m_state.modified = false;

            await this.domain.users.setTypedPreference(null, DashboardManagementService.NavigationHistoryKeyName, this.m_state);
        }
    }

    private static fixupPrototype(state: NavigationHistory): NavigationHistory
    {
        if (state)
        {
            Object.setPrototypeOf(state, NavigationHistory.prototype);
        }

        return state;
    }
}

export class NavigationHistory
{
    modified: boolean;

    customers: string[];
    services: string[];
    hosts: string[];
    agents: string[];

    recordCustomer(cust: CustomerExtended)
    {
        this.customers = this.updateLRU(this.customers, cust.model.sysId);
    }

    recordService(svc: CustomerServiceExtended)
    {
        this.services = this.updateLRU(this.services, svc.model.sysId);
    }

    recordHost(host: DeploymentHostExtended)
    {
        this.hosts = this.updateLRU(this.hosts, host.model.sysId);
    }

    recordAgent(agent: DeploymentAgentExtended)
    {
        this.agents = this.updateLRU(this.agents, agent.model.sysId);
    }

    async toObjects<M extends BaseModel, E extends ExtendedModel<M>>(svc: SharedSvc.BaseService<M, E>,
                                                                     ids: string[],
                                                                     callback: (sysId: string) => Models.RecordIdentity): Promise<E[]>
    {
        ids = ids || [];

        let ris  = ids.map((sysId) => callback(sysId));
        let exts = await svc.getExtendedBatch(ris);

        if (exts.length != ids.length)
        {
            ids.splice(0, ids.length);
            for (let ext of exts)
            {
                ids.push(ext.model.sysId);
            }

            this.modified = true;
        }

        return exts;
    }

    //--//

    private updateLRU(array: string[],
                      sysId: string): string[]
    {
        array = array || [];

        if (array[0] == sysId)
        {
            return array;
        }

        let res = [sysId];

        for (let other of array)
        {
            if (other != sysId)
            {
                res.push(other);
            }

            if (res.length >= 30)
            {
                break;
            }
        }

        this.modified = true;

        return res;
    }
}
