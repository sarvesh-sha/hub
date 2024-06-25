import {Component} from "@angular/core";
import * as Models from "app/services/proxy/model/models";
import {LocationFiltersAdapter} from "app/shared/filter/filters-adapter";

@Component({
               selector   : "o3-gateway-filters-adapter[request]",
               templateUrl: "./gateway-filters-adapter.component.html"
           })
export class GatewayFiltersAdapterComponent extends LocationFiltersAdapter<Models.GatewayFilterRequest>
{
    protected emptyRequestInstance(): Models.GatewayFilterRequest
    {
        return new Models.GatewayFilterRequest();
    }

    protected newRequestInstance(request?: Models.GatewayFilterRequest): Models.GatewayFilterRequest
    {
        return Models.GatewayFilterRequest.newInstance(request);
    }
}
