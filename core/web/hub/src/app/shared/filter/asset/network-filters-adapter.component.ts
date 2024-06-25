import {Component} from "@angular/core";
import * as Models from "app/services/proxy/model/models";
import {LocationFiltersAdapter} from "app/shared/filter/filters-adapter";

@Component({
               selector   : "o3-network-filters-adapter[request]",
               templateUrl: "./network-filters-adapter.component.html"
           })
export class NetworkFiltersAdapterComponent extends LocationFiltersAdapter<Models.NetworkFilterRequest>
{
    protected emptyRequestInstance(): Models.NetworkFilterRequest
    {
        return new Models.NetworkFilterRequest();
    }

    protected newRequestInstance(request?: Models.NetworkFilterRequest): Models.NetworkFilterRequest
    {
        return Models.NetworkFilterRequest.newInstance(request);
    }
}
