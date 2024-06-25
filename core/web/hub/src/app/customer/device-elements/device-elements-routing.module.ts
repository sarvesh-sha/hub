import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";

import {DeviceElementsListPageComponent} from "app/customer/device-elements/device-elements-list-page.component";
import {AppNavigationResolver} from "app/app-navigation.resolver";

const routes: Routes = [
    {
        path    : "",
        children: [
            {
                path      : "",
                redirectTo: "summary",
                pathMatch : "full"
            },
            {
                path     : "summary",
                component: DeviceElementsListPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Control Points",
                    breadcrumbs: []
                }
            }
        ]
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class DeviceElementsRoutingModule {}
