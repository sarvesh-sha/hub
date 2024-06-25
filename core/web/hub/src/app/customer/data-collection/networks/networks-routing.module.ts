import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {AppNavigationResolver} from "app/app-navigation.resolver";
import {NetworksDetailPageComponent} from "app/customer/data-collection/networks/networks-detail-page.component";
import {NetworksSummaryPageComponent} from "app/customer/data-collection/networks/networks-summary-page.component";

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
                component: NetworksSummaryPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Networks",
                    breadcrumbs: []
                }
            },
            {
                path     : "network/:id",
                component: NetworksDetailPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Networks",
                    breadcrumbs: [
                        {
                            title: "Network Summary",
                            url  : "/networks/summary"
                        }
                    ]
                }
            }
        ]
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class NetworksRoutingModule {}
