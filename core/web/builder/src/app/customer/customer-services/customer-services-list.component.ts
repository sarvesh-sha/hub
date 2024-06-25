import {Component, Injector, Input} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";

import {CustomerServiceExtended} from "app/services/domain/customer-services.service";
import {CustomerExtended} from "app/services/domain/customers.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-customer-services-list",
               templateUrl: "./customer-services-list.component.html"
           })
export class CustomerServicesListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, CustomerServiceExtended, ServiceDecoded>
{
    private m_extended: CustomerExtended;

    public get extended(): CustomerExtended
    {
        return this.m_extended;
    }

    @Input()
    public set extended(value: CustomerExtended)
    {
        this.m_extended = value;
        this.table.refreshData();
    }

    //--//

    table: DatatableManager<Models.RecordIdentity, CustomerServiceExtended, ServiceDecoded>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = this.newTableWithAutoRefresh(this.app.domain.customerServices, this);
    }

    getItemName(): string
    {
        return "Customer Services";
    }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        if (!this.extended || !this.extended.model.services)
        {
            return [];
        }

        return this.extended.model.services;
    }

    getPage(offset: number,
            limit: number): Promise<CustomerServiceExtended[]>
    {
        return this.app.domain.customerServices.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: CustomerServiceExtended[]): Promise<ServiceDecoded[]>
    {
        return mapInParallel(rows, async (row) =>
        {
            let decoded  = new ServiceDecoded(row);
            let activity = await row.getCurrentRunningActivity();
            if (activity)
            {
                decoded.pendingActivity = activity.model.title;
            }

            let jobs     = await row.extractJobs();
            decoded.jobs = jobs.sort((a,
                                      b) => MomentHelper.compareDates(a.model.createdOn, b.model.createdOn))
                               .map(j => j.model.idPrefix)
                               .join(" / ");

            return decoded;
        });
    }

    itemClicked(columnId: string,
                item: ServiceDecoded)
    {
        this.app.ui.navigation.push([
                                        "service",
                                        item.ext.model.sysId
                                    ]);
    }
}

class ServiceDecoded
{
    pendingActivity: string;
    jobs: string;

    constructor(public ext: CustomerServiceExtended)
    {
    }

    get heapStatus(): string
    {
        return this.ext.model.heapStatusAbnormal ? "Needs Attention" : "Normal";
    }
}
