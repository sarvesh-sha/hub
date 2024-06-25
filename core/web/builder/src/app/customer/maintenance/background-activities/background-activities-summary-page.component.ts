import {Component, ViewChild} from "@angular/core";

import {AppService} from "app/app.service";
import {BackgroundActivitiesListComponent} from "app/customer/maintenance/background-activities/background-activities-list.component";

@Component({
               selector   : "o3-jobs-summary-page",
               templateUrl: "./background-activities-summary-page.component.html"
           })
export class BackgroundActivitiesSummaryPageComponent
{
    @ViewChild("childList", {static: true}) childList: BackgroundActivitiesListComponent;

    constructor(public appService: AppService)
    {
    }
}
