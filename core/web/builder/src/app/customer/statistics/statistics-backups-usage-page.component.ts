﻿import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";
import {CustomerExtended} from "app/services/domain/customers.service";
import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";
import {ColumnConfiguration, DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {inParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-statistics-backups-usage-page",
               templateUrl: "./statistics-backups-usage-page.component.html"
           })
export class StatisticsBackupsUsagePageComponent extends SharedSvc.BaseApplicationComponent
{
    backupsDetails: BackupDetails[];

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
        let customers                       = await this.app.domain.customers.getExtendedAll();
        let backupsDetails: BackupDetails[] = [];

        await inParallel(customers, async (customer) =>
        {
            for (let svc of await customer.getServices())
            {
                for (let backup of await this.app.domain.customerServiceBackups.getExtendedBatch(svc.model.backups))
                {
                    let backupDetails = backupsDetails.find((v) => v.kind == backup.model.trigger);
                    if (!backupDetails)
                    {
                        backupDetails      = new BackupDetails(this, backup.model.trigger);
                        backupDetails.kind = backup.model.trigger;

                        backupsDetails.push(backupDetails);
                    }

                    backupDetails.count++;

                    let serviceDetails = backupDetails.services.find((s) => s.service.sameIdentity(svc));
                    if (!serviceDetails)
                    {
                        serviceDetails = new ServiceDetails();

                        serviceDetails.service  = svc;
                        serviceDetails.customer = await svc.getOwningCustomer();

                        backupDetails.services.push(serviceDetails);
                    }

                    serviceDetails.count++;
                }
            }
        });

        backupsDetails.sort((a,
                             b) => b.count - a.count);

        for (let backupDetails of backupsDetails)
        {
            backupDetails.services.sort((a,
                                         b) =>
                                        {
                                            return UtilsService.compareStrings(a.serviceName, b.serviceName, true);
                                        });
        }

        this.backupsDetails = backupsDetails;
    }
}

class BackupDetails implements IDatatableDataProvider<ServiceDetails, ServiceDetails, ServiceDetails>
{
    count: number = 0;

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

    constructor(private component: StatisticsBackupsUsagePageComponent,
                public kind: Models.BackupKind)
    {
        this.table = new DatatableManager<ServiceDetails, ServiceDetails, ServiceDetails>(this, () =>
        {
            return this.component.getViewState()
                       .getSubView(kind, true);
        });
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
    count: number = 0;

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