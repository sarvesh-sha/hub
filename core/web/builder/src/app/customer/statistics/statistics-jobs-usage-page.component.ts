import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";
import {CustomerExtended} from "app/services/domain/customers.service";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {ColumnConfiguration, DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-statistics-jobs-usage-page",
               templateUrl: "./statistics-jobs-usage-page.component.html"
           })
export class StatisticsJobsUsagePageComponent extends SharedSvc.BaseApplicationComponent
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
            let entry = new JobDetails(this);
            entry.job = jobUsage;

            let lookup: { [key: string]: ServiceDetails } = {};

            for (let v of jobUsage.servicesInRole || [])
            {
                let role = v.role;
                for (let svc of await this.app.domain.customerServices.getExtendedBatch(v.entries))
                {
                    let serviceDetails = lookup[svc.model.sysId];
                    if (!serviceDetails)
                    {
                        serviceDetails          = new ServiceDetails();
                        serviceDetails.service  = svc;
                        serviceDetails.customer = await svc.getOwningCustomer();

                        lookup[svc.model.sysId] = serviceDetails;

                        entry.services.push(serviceDetails);
                    }

                    serviceDetails.roles.push(role);
                }
            }

            if (entry.services.length > 0)
            {
                entry.services.sort((a,
                                     b) =>
                                    {
                                        return UtilsService.compareStrings(a.serviceName, b.serviceName, true);
                                    });

                usages.push(entry);
            }
        }

        this.usages = usages;
    }
}

class JobDetails implements IDatatableDataProvider<ServiceDetails, ServiceDetails, ServiceDetails>
{
    job: Models.JobUsage;

    services: ServiceDetails[] = [];

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

    table: DatatableManager<ServiceDetails, ServiceDetails, ServiceDetails>;
    private m_tableBound = false;

    constructor(private component: StatisticsJobsUsagePageComponent)
    {
        this.table = new DatatableManager<ServiceDetails, ServiceDetails, ServiceDetails>(this, () => null);
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

    public async setColumnConfigs(configs: ColumnConfiguration[]): Promise<boolean>
    {
        return true;
    }

    public async getColumnConfigs(): Promise<ColumnConfiguration[]>
    {
        return null;
    }

    public getItemName(): string
    {
        return "Services";
    }

    public async getList(): Promise<ServiceDetails[]>
    {
        return this.services.filter((serviceDetail) =>
                                    {
                                        if (this.m_filter)
                                        {
                                            if (this.contains(serviceDetail.serviceName))
                                            {
                                                return true;
                                            }

                                            if (this.contains(serviceDetail.customerName))
                                            {
                                                return true;
                                            }

                                            for (let role of serviceDetail.roles)
                                            {
                                                let roleText = <string><any>role;

                                                if (this.contains(roleText))
                                                {
                                                    return true;
                                                }
                                            }

                                            return false;
                                        }

                                        return true;
                                    });
    }

    public async getPage(offset: number,
                         limit: number): Promise<ServiceDetails[]>
    {
        return this.table.slicePage(offset, limit);
    }

    public itemClicked(columnId: string,
                       item: ServiceDetails)
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
        }
    }

    public async transform(rows: ServiceDetails[]): Promise<ServiceDetails[]>
    {
        return rows;
    }

    private contains(val: string)
    {
        return val && val.toLowerCase()
                         .indexOf(this.m_filter.toLowerCase()) >= 0;
    }
}

class ServiceDetails
{
    roles: Models.DeploymentRole[] = [];

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

    get rolesText(): string
    {
        return this.roles.join(" / ");
    }
}
