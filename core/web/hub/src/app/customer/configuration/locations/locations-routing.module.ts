import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {AppNavigationResolver} from "app/app-navigation.resolver";
import {LocationDetailPageComponent} from "app/customer/configuration/locations/location-detail-page.component";
import {LocationListPageComponent} from "app/customer/configuration/locations/location-list-page.component";

const routes: Routes = [
    {
        path     : "",
        component: LocationListPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "Locations",
            breadcrumbs: []
        }
    },
    {
        path     : "location/:id",
        component: LocationDetailPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "Locations",
            breadcrumbs: [
                {
                    title: "Manage Locations",
                    url  : "configuration/locations"
                }
            ]
        }
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class LocationsRoutingModule {}
