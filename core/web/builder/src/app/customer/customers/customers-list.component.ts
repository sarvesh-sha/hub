import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {CustomerExtended} from "app/services/domain/customers.service";
import {JobExtended} from "app/services/domain/jobs.service";
import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-customers-list",
               templateUrl: "./customers-list.component.html"
           })
export class CustomersListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, CustomerExtended, CustomerDecoded>
{
    table: DatatableManager<Models.RecordIdentity, CustomerExtended, CustomerDecoded>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table       = this.newTableWithAutoRefresh(this.app.domain.customers, this);
        this.table.limit = 20;
    }

    getItemName(): string
    {
        return "Customers";
    }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        let ids = await this.app.domain.customers.getList();
        return ids;
    }

    getPage(offset: number,
            limit: number): Promise<CustomerExtended[]>
    {
        return this.app.domain.customers.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: CustomerExtended[]): Promise<CustomerDecoded[]>
    {
        return mapInParallel(rows, async (row) =>
        {
            let decoded = new CustomerDecoded(row);

            let instanceTypes       = new Set<Models.DeploymentInstance>();
            let jobs: JobExtended[] = [];

            for (let svc of await row.getServices())
            {
                let instanceType = svc.model.instanceType;
                if (instanceType)
                {
                    instanceTypes.add(instanceType);
                }

                for (let job of await svc.extractJobs())
                {
                    if (!jobs.find(j => j.sameIdentity(job)))
                    {
                        jobs.push(job);
                    }
                }
            }

            decoded.jobs = jobs.sort((a,
                                      b) => MomentHelper.compareDates(a.model.createdOn, b.model.createdOn))
                               .map(j => j.model.idPrefix)
                               .join(" / ");

            decoded.instances = [...instanceTypes.values()].sort((a,
                                                                  b) => UtilsService.compareStrings(a, b, true))
                                                           .join(" / ");

            return decoded;
        });
    }

    itemClicked(columnId: string,
                item: CustomerDecoded)
    {
        this.app.ui.navigation.push([
                                        "..",
                                        "item",
                                        item.ext.model.sysId
                                    ]);
    }
}

class CustomerDecoded
{
    jobs: string;
    instances: string;

    constructor(public ext: CustomerExtended)
    {
    }
}
