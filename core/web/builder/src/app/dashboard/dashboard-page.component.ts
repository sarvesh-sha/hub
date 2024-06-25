import {Component, Injector} from "@angular/core";
import {DashboardManagementService} from "app/dashboard/dashboard-management.service";

import * as SharedSvc from "app/services/domain/base.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";
import {CustomerExtended} from "app/services/domain/customers.service";
import {DeploymentAgentExtended} from "app/services/domain/deployment-agents.service";
import {DeploymentHostExtended} from "app/services/domain/deployment-hosts.service";
import {Memoizer, ResetMemoizers} from "framework/utils/memoizers";

@Component({
               selector   : "o3-dashboard-page",
               templateUrl: "./dashboard-page.component.html",
               styleUrls  : ["./dashboard-page.component.scss"]
           })
export class DashboardPageComponent extends SharedSvc.BaseApplicationComponent
{
    private svc: DashboardManagementService;

    private m_limitCustomers: number = 3;
    private m_limitServices: number  = 3;
    private m_limitHosts: number     = 3;
    private m_limitAgents: number    = 3;

    hasMoreCustomers: boolean;
    hasMoreServices: boolean;
    hasMoreHosts: boolean;
    hasMoreAgents: boolean;

    constructor(inj: Injector)
    {
        super(inj);

        this.svc = this.inject(DashboardManagementService);
    }

    @ResetMemoizers
    public increaseCustomerLimit()
    {
        this.m_limitCustomers += 5;
    }

    @ResetMemoizers
    public increaseServiceLimit()
    {
        this.m_limitServices += 5;
    }

    @ResetMemoizers
    public increaseHostLimit()
    {
        this.m_limitHosts += 5;
    }

    @ResetMemoizers
    public increaseAgentLimit()
    {
        this.m_limitAgents += 5;
    }

    //--//

    @Memoizer
    async getLastCustomers(): Promise<CustomerDetails[]>
    {
        let customers = DashboardPageComponent.limit(await this.svc.getLastCustomers(), this.m_limitCustomers, (val) => this.hasMoreCustomers = val);

        return customers.map((cust) => new CustomerDetails(cust));
    }

    @Memoizer
    async getLastServices(): Promise<ServiceDetails[]>
    {
        let services = DashboardPageComponent.limit(await this.svc.getLastServices(), this.m_limitServices, (val) => this.hasMoreServices = val);

        return services.map((svc) => new ServiceDetails(svc));
    }

    @Memoizer
    async getLastHosts(): Promise<HostDetails[]>
    {
        let hosts = DashboardPageComponent.limit(await await this.svc.getLastHosts(), this.m_limitHosts, (val) => this.hasMoreHosts = val);

        return hosts.map((host) => new HostDetails(host));
    }

    @Memoizer
    async getLastAgents(): Promise<AgentDetails[]>
    {
        let agents = DashboardPageComponent.limit(await await this.svc.getLastAgents(), this.m_limitAgents, (val) => this.hasMoreAgents = val);

        return agents.map((agent) => new AgentDetails(agent));
    }

    //--//

    goToCustomer(ext: CustomerExtended)
    {
        this.app.ui.navigation.go("/customers", [
            "item",
            ext.model.sysId
        ]);
    }

    async goToService(ext: CustomerServiceExtended)
    {
        let cust = await ext.getOwningCustomer();

        this.app.ui.navigation.go("/customers", [
            "item",
            cust.model.sysId,
            "service",
            ext.model.sysId
        ]);
    }

    goToHost(ext: DeploymentHostExtended)
    {
        this.app.ui.navigation.go("/deployments", [
            "item",
            ext.model.sysId
        ]);
    }

    async goToAgent(ext: DeploymentAgentExtended)
    {
        let host = await ext.getOwningDeployment();

        this.app.ui.navigation.go("/deployments", [
            "item",
            host.model.sysId,
            "agent",
            ext.model.sysId
        ]);
    }

    //--//

    private static limit<T>(exts: T[],
                            limit: number,
                            callback: (hasMore: boolean) => void): T[]
    {
        let hasMore = limit < exts.length;

        callback(hasMore);

        return hasMore ? exts.slice(0, limit) : exts;
    }
}

class CustomerDetails
{
    constructor(public ext: CustomerExtended)
    {
    }
}

class ServiceDetails
{
    constructor(public ext: CustomerServiceExtended)
    {
    }

    @Memoizer
    async getCustomerName(): Promise<string>
    {
        let cust = await this.ext.getOwningCustomer();
        if (cust)
        {
            return cust.model.name;
        }

        return "<no customer>";
    }
}

class HostDetails
{
    constructor(public ext: DeploymentHostExtended)
    {
    }

    @Memoizer
    async getServiceName(): Promise<string>
    {
        let svc = await this.ext.getCustomerService();
        if (svc)
        {
            return svc.model.name;
        }

        return "<no service>";
    }

    @Memoizer
    async getCustomerName(): Promise<string>
    {
        let svc = await this.ext.getCustomerService();
        if (svc)
        {
            let cust = await svc.getOwningCustomer();
            if (cust)
            {
                return cust.model.name;
            }
        }

        return "<no customer>";
    }
}

class AgentDetails
{
    constructor(public ext: DeploymentAgentExtended)
    {
    }

    @Memoizer
    async getHostName(): Promise<string>
    {
        let host = await this.ext.getOwningDeployment();

        return host.displayName;
    }

    @Memoizer
    async getServiceName(): Promise<string>
    {
        let host = await this.ext.getOwningDeployment();
        let svc  = await host.getCustomerService();
        if (svc)
        {
            return svc.model.name;
        }

        return "<no service>";
    }

    @Memoizer
    async getCustomerName(): Promise<string>
    {
        let host = await this.ext.getOwningDeployment();
        let svc  = await host.getCustomerService();
        if (svc)
        {
            let cust = await svc.getOwningCustomer();
            if (cust)
            {
                return cust.model.name;
            }
        }

        return "<no customer>";
    }
}
