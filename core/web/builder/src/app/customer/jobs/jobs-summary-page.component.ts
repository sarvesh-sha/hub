import {Component, ViewChild} from "@angular/core";
import {ActivatedRoute} from "@angular/router";

import {AppService} from "app/app.service";
import {JobsListComponent} from "app/customer/jobs/jobs-list.component";

@Component({
               selector   : "o3-jobs-summary-page",
               templateUrl: "./jobs-summary-page.component.html"
           })
export class JobsSummaryPageComponent
{
    @ViewChild("childList", {static: true}) childList: JobsListComponent;

    constructor(public activatedRoute: ActivatedRoute,
                public appService: AppService)
    {
    }
}
