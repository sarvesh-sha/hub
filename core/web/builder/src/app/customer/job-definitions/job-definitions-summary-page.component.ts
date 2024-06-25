import {Component, Injector, ViewChild} from "@angular/core";

import {JobDefinitionsListComponent} from "app/customer/job-definitions/job-definitions-list.component";

import * as SharedSvc from "app/services/domain/base.service";

@Component({
               selector   : "o3-job-definitions-summary-page",
               templateUrl: "./job-definitions-summary-page.component.html"
           })
export class JobDefinitionsSummaryPageComponent extends SharedSvc.BaseApplicationComponent
{
    @ViewChild("childList", { static: true })childList: JobDefinitionsListComponent;

    constructor(inj: Injector)
    {
        super(inj);
    }

    newJobDefinition()
    {
    }
}
