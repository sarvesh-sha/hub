import {Component, Injector, ViewChild} from "@angular/core";

import {RepositoriesListComponent} from "app/customer/repositories/repositories-list.component";

import * as SharedSvc from "app/services/domain/base.service";

@Component({
               selector   : "o3-repositories-summary-page",
               templateUrl: "./repositories-summary-page.component.html"
           })
export class RepositoriesSummaryPageComponent extends SharedSvc.BaseApplicationComponent
{
    @ViewChild("childList", { static: true })repositoriesList: RepositoriesListComponent;

    constructor(inj: Injector)
    {
        super(inj);
    }

    newRepo()
    {
    }
}
