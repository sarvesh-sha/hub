import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

@Component({
               selector   : "o3-statistics-cellular-chargers-page",
               templateUrl: "./statistics-cellular-charges-page.component.html"
           })
export class StatisticsCellularChargesPageComponent extends SharedSvc.BaseApplicationComponent
{
    charges: Models.DeploymentCellularChargesSummary;

    //--//

    constructor(inj: Injector)
    {
        super(inj);
    }

    public ngAfterViewInit(): void
    {
        super.ngAfterViewInit();

        this.fetchCharges();
    }

    async fetchCharges()
    {
        this.charges = await this.app.domain.customerServices.getAllCharges(100);
    }

    getChargesReport()
    {
        let timestamp = MomentHelper.fileNameFormat();

        let url = this.app.domain.apis.customers.getAllChargesReport__generateUrl(`charges__${timestamp}.csv`);

        window.open(url, "_blank");
    }
}
