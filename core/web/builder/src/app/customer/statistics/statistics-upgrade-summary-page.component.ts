import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";
import {CustomerExtended} from "app/services/domain/customers.service";
import {UtilsService} from "framework/services/utils.service";
import {inParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-statistics-upgrade-summary-page",
               templateUrl: "./statistics-upgrade-summary-page.component.html"
           })
export class StatisticsUpgradeSummaryPageComponent extends SharedSvc.BaseApplicationComponent
{
    detailsNoApi: ServiceStatus[];
    detailsUpToDate: ServiceStatus[];
    missingLookup: { [fixup: string]: ServiceStatus[] };

    get missingLookupKeys(): string[]
    {
        return UtilsService.extractKeysFromMap(this.missingLookup);
    }

    constructor(inj: Injector)
    {
        super(inj);
    }

    public ngAfterViewInit(): void
    {
        super.ngAfterViewInit();

        this.fetchUpgradeSummary();
    }

    async fetchUpgradeSummary()
    {
        let details: ServiceStatus[]         = [];
        let detailsNoApi: ServiceStatus[]    = [];
        let detailsUpToDate: ServiceStatus[] = [];

        let fixups = new Set<string>();

        let customerReports = await this.app.domain.apis.adminTasks.checkUpgradeLevelForServices();
        await inParallel(customerReports,
                         async (customerReport,
                                index) =>
                         {
                             let cust = await this.app.domain.customers.getExtendedByIdentity(customerReport.customer);

                             await inParallel(customerReport.services,
                                              async (serviceReport,
                                                     index) =>
                                              {
                                                  let svcStatus     = new ServiceStatus();
                                                  svcStatus.custExt = cust;
                                                  svcStatus.svcExt  = await this.app.domain.customerServices.getExtendedByIdentity(serviceReport.service);

                                                  if (serviceReport.fixupProcessors)
                                                  {
                                                      details.push(svcStatus);

                                                      for (let fixup of serviceReport.fixupProcessors)
                                                      {
                                                          svcStatus.fixups.add(fixup);
                                                          fixups.add(fixup);
                                                      }
                                                  }
                                                  else
                                                  {
                                                      detailsNoApi.push(svcStatus);
                                                  }
                                              });
                         });

        let missing: { [fixup: string]: ServiceStatus[] } = {};

        for (let svcStatus of details)
        {
            let upToDate = true;

            for (let fixup of fixups.values())
            {
                if (!svcStatus.fixups.has(fixup))
                {
                    let array = missing[fixup] || [];
                    array.push(svcStatus);
                    missing[fixup] = array;
                    upToDate       = false;
                }
            }

            if (upToDate)
            {
                detailsUpToDate.push(svcStatus);
            }
        }

        this.missingLookup   = missing;
        this.detailsNoApi    = detailsNoApi;
        this.detailsUpToDate = detailsUpToDate;
    }
}

class ServiceStatus
{
    custExt: CustomerExtended;
    svcExt: CustomerServiceExtended;

    fixups: Set<string> = new Set<string>();

    static sort(array: ServiceStatus[])
    {
        array.sort((a,
                    b) =>
                   {
                       let diff = UtilsService.compareStrings(a.custExt.model.name, b.custExt.model.name, true);
                       if (diff == 0)
                       {
                           diff = UtilsService.compareStrings(a.svcExt.model.name, b.svcExt.model.name, true);
                       }
                       return diff;
                   });
    }
}
