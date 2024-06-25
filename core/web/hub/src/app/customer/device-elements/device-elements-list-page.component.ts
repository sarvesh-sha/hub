import {Component, Injector, ViewChild} from "@angular/core";

import {DeviceElementsListComponent} from "app/customer/device-elements/device-elements-list.component";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {DeviceElementFiltersAdapterComponent} from "app/shared/filter/asset/device-element-filters-adapter.component";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";


@Component({
               selector   : "o3-device-elements-list-page",
               templateUrl: "./device-elements-list-page.component.html"
           })
export class DeviceElementsListPageComponent extends BaseApplicationComponent
{
    filtersLoaded: boolean;
    limitExceeded: boolean;
    filters             = new Models.DeviceElementFilterRequest();
    chips: FilterChip[] = [];

    //--//

    @ViewChild(DeviceElementsListComponent) controlPointsList: DeviceElementsListComponent;
    @ViewChild(DeviceElementFiltersAdapterComponent) filtersAdapter: DeviceElementFiltersAdapterComponent;

    constructor(inj: Injector)
    {
        super(inj);
    }

    async refresh()
    {
        if (!this.filtersLoaded) this.filtersLoaded = true;
    }
}
