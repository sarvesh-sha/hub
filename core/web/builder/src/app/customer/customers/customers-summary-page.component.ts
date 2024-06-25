import {Component, ViewChild} from "@angular/core";
import {UUID} from "angular2-uuid";

import {CustomersListComponent} from "app/customer/customers/customers-list.component";

import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {DownloadDialogComponent, DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";

import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-customers-summary-page",
               templateUrl: "./customers-summary-page.component.html"
           })
export class CustomersSummaryPageComponent extends SharedSvc.BaseApplicationComponent
{
    @ViewChild("childList", {static: true}) customersList: CustomersListComponent;

    //--//

    checkUsagesDialogConfig = OverlayConfig.onTopDraggable({
                                                               width : "50vw",
                                                               height: "90vh"
                                                           });

    checkUsagesFilter: string;
    checkUsagesMaxResults: number;
    checkUsagesCaseInsensitive: boolean;
    checkUsagesResults: string = "";

    //--//

    newCustomer()
    {
        this.app.ui.navigation.push([
                                        "..",
                                        "item",
                                        "new"
                                    ]);
    }

    getInventory()
    {
        DownloadDialogComponent.openWithGenerator(this, "Inventory Report", DownloadDialogComponent.fileName("Inventory", ".xlsx"), new InventoryDownloader(this.app.domain.apis));
    }

    //--//

    isValidCheckUsages()
    {
        return !!this.checkUsagesFilter;
    }

    async checkUsages()
    {
        this.checkUsagesResults = "";

        let filters = Models.UsageFilterRequest.newInstance({
                                                                items          : this.checkUsagesFilter.split(" "),
                                                                maxResults     : this.checkUsagesMaxResults,
                                                                caseInsensitive: this.checkUsagesCaseInsensitive
                                                            });

        await mapInParallel(await this.app.domain.customers.getExtendedAll(), async (customer) =>
        {
            await mapInParallel(await customer.getServices(), async (svc) =>
            {
                let response = await svc.checkUsages(filters);
                let lines    = svc.formatUsages(response, true);

                if (lines)
                {
                    let append = `${customer.model.name} / ${svc.model.name}:\n`;

                    for (let line of lines)
                    {
                        append += `    ${line}\n`;
                    }

                    append += "\n";

                    this.checkUsagesResults += append;
                }
            });
        });

        if (this.checkUsagesResults == "")
        {
            this.checkUsagesResults = "No hits!";
        }
        else
        {
            this.checkUsagesResults += "--------------\n";
            this.checkUsagesResults += "Search done!";
        }
    }
}

class InventoryDownloader implements DownloadGenerator
{
    constructor(private apis: ApiService)
    {
    }

    public getProgressPercent()
    {
        return NaN;
    }

    public getProgressMessage()
    {
        return "Generating Report...";
    }

    public async makeProgress(dialog: DownloadDialogComponent): Promise<boolean>
    {
        return true;
    }

    public async sleepForProgress(): Promise<void>
    {
        // We don't need to sleep.
    }

    public isDeterminate()
    {
        return false;
    }

    public async getResults(): Promise<DownloadResults>
    {
        let contents = await this.apis.deploymentHosts.reportInventory(UUID.UUID());
        return {blob: contents};
    }
}
