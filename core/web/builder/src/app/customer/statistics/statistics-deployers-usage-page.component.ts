import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";
import {CustomerExtended} from "app/services/domain/customers.service";
import {DeploymentHostExtended} from "app/services/domain/deployment-hosts.service";

import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";
import {ColumnConfiguration, DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {inParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-statistics-deployers-usage-page",
               templateUrl: "./statistics-deployers-usage-page.component.html"
           })
export class StatisticsDeployersUsagePageComponent extends SharedSvc.BaseApplicationComponent
{
    usages: JobDetails[];

    //--//

    constructor(inj: Injector)
    {
        super(inj);
    }

    public ngAfterViewInit(): void
    {
        super.ngAfterViewInit();

        this.fetchJobUsage();
    }

    async fetchJobUsage()
    {
        let jobUsages = await this.app.domain.jobs.computeJobUsage();

        let usages = [];

        for (let jobUsage of jobUsages)
        {
            for (let v of jobUsage.hostsInRole || [])
            {
                if (v.role == Models.DeploymentRole.deployer)
                {
                    let hosts = await this.app.domain.deploymentHosts.getExtendedBatch(v.entries);
                    if (hosts && hosts.length > 0)
                    {
                        let entry = new JobDetails(this);
                        entry.job = jobUsage;

                        hosts.sort((a,
                                    b) =>
                                   {
                                       return UtilsService.compareStrings(a.displayName, b.displayName, true);
                                   });

                        for (let host of hosts)
                        {
                            let hostDetails = new HostDetails();
                            hostDetails.ext = host;
                            entry.hosts.push(hostDetails);
                        }

                        await inParallel(entry.hosts, async (hostDetails) =>
                        {
                            hostDetails.service = await hostDetails.ext.getCustomerService();
                            if (hostDetails.service)
                            {
                                hostDetails.customer = await hostDetails.service.getOwningCustomer();
                            }
                        });

                        usages.push(entry);
                    }
                }
            }
        }

        this.usages = usages;
    }
}

class JobDetails implements IDatatableDataProvider<HostDetails, HostDetails, HostDetails>
{
    job: Models.JobUsage;

    hosts: HostDetails[] = [];

    private m_filter: string;

    get filter(): string
    {
        return this.m_filter;
    }

    set filter(value: string)
    {
        this.m_filter = value;

        this.table.refreshData();
    }

    table: DatatableManager<HostDetails, HostDetails, HostDetails>;
    private m_tableBound = false;

    constructor(private component: StatisticsDeployersUsagePageComponent)
    {
        this.table = new DatatableManager<HostDetails, HostDetails, HostDetails>(this, () => null);
    }

    bindTable()
    {
        if (!this.m_tableBound)
        {
            this.m_tableBound = true;

            this.table.refreshData();
        }
    }

    //--//

    public wasDestroyed(): boolean
    {
        return this.component.wasDestroyed();
    }

    public detectChanges()
    {
        this.component.detectChanges();
    }

    //--//

    public getTableConfigId(): string
    {
        return null;
    }

    async setColumnConfigs(configs: ColumnConfiguration[]): Promise<boolean>
    {
        return true;
    }

    async getColumnConfigs(): Promise<ColumnConfiguration[]>
    {
        return null;
    }

    public getItemName(): string
    {
        return "Hosts";
    }

    public async getList(): Promise<HostDetails[]>
    {
        return this.hosts.filter((hostDetail) =>
                                 {
                                     if (this.m_filter)
                                     {
                                         if (this.contains(hostDetail.ext.displayName))
                                         {
                                             return true;
                                         }

                                         if (this.contains(hostDetail.serviceName))
                                         {
                                             return true;
                                         }

                                         if (this.contains(hostDetail.customerName))
                                         {
                                             return true;
                                         }

                                         return false;
                                     }

                                     return true;
                                 });
    }

    public async getPage(offset: number,
                         limit: number): Promise<HostDetails[]>
    {
        return this.table.slicePage(offset, limit);
    }

    public itemClicked(columnId: string,
                       item: HostDetails)
    {
        switch (columnId)
        {
            case "customerName":
                if (item.customer)
                {
                    this.component.app.ui.navigation.go("/customers/item", [
                        item.customer.model.sysId
                    ]);
                }
                break;

            case "serviceName":
                if (item.service)
                {
                    this.component.app.ui.navigation.go("/customers/item", [
                        item.customer.model.sysId,
                        "service",
                        item.service.model.sysId
                    ]);
                }
                break;

            default:
            case "displayName":
                this.component.app.ui.navigation.go("/deployments", [
                    "item",
                    item.ext.model.sysId
                ]);
        }
    }

    public async transform(rows: HostDetails[]): Promise<HostDetails[]>
    {
        return rows;
    }

    private contains(val: string)
    {
        return val && val.toLowerCase()
                         .indexOf(this.m_filter.toLowerCase()) >= 0;
    }
}

class HostDetails
{
    ext: DeploymentHostExtended;

    customer: CustomerExtended;

    service: CustomerServiceExtended;

    get customerName(): string
    {
        return this.customer ? this.customer.model.name : "<no-customer>";
    }

    get serviceName(): string
    {
        return this.service ? this.service.model.name : "<no-service>";
    }
}
