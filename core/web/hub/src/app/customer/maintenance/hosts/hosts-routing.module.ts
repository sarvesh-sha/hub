import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";

import {AppNavigationResolver} from "app/app-navigation.resolver";
import {HostsDetailPageLogComponent} from "app/customer/maintenance/hosts/hosts-detail-page-log.component";
import {HostsDetailPageComponent} from "app/customer/maintenance/hosts/hosts-detail-page.component";
import {HostsSummaryPageComponent} from "app/customer/maintenance/hosts/hosts-summary-page.component";

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
                component: HostsSummaryPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Hosts",
                    breadcrumbs: []
                }
            },
            {
                path     : "host/:id",
                component: HostsDetailPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Hosts",
                    breadcrumbs: [
                        {
                            title: "Host Summary",
                            url  : "/hosts/summary"
                        }
                    ]
                }
            },
            {
                path     : "host/:id/log",
                component: HostsDetailPageLogComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Hosts",
                    breadcrumbs: [
                        {
                            title: "Host Summary",
                            url  : "/hosts/summary"
                        },
                        {
                            title: "Host Details",
                            url  : "/hosts/host/:id"
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
export class HostsRoutingModule {}
