import {Component, Injector, ViewChild} from "@angular/core";

import {HostsListComponent} from "app/customer/hosts/hosts-list/hosts-list.component";

import * as SharedSvc from "app/services/domain/base.service";

@Component({
               selector   : "o3-hosts-summary-page",
               templateUrl: "./hosts-summary-page.component.html"
           })
export class HostsSummaryPageComponent extends SharedSvc.BaseApplicationComponent
{
    @ViewChild(HostsListComponent) hostsList: HostsListComponent;

    constructor(inj: Injector)
    {
        super(inj);
    }
}
