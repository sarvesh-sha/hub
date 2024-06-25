import {Component} from "@angular/core";
import * as Models from "app/services/proxy/model/models";
import {LocationFiltersAdapter} from "app/shared/filter/filters-adapter";

@Component({
               selector   : "o3-host-filters-adapter[request]",
               templateUrl: "./host-filters-adapter.component.html"
           })
export class HostFiltersAdapterComponent extends LocationFiltersAdapter<Models.HostFilterRequest>
{
    protected emptyRequestInstance(): Models.HostFilterRequest
    {
        return new Models.HostFilterRequest();
    }

    protected newRequestInstance(request?: Models.HostFilterRequest): Models.HostFilterRequest
    {
        return Models.HostFilterRequest.newInstance(request);
    }
}
