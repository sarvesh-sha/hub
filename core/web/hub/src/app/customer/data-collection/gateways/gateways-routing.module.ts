import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {AppNavigationResolver} from "app/app-navigation.resolver";
import {GatewaysDetailPageLogComponent} from "app/customer/data-collection/gateways/gateways-detail-page-log.component";
import {GatewaysDetailPageComponent} from "app/customer/data-collection/gateways/gateways-detail-page.component";
import {GatewaysSummaryPageComponent} from "app/customer/data-collection/gateways/gateways-summary-page.component";
import {ProbersDetailPageComponent} from "app/customer/data-collection/gateways/probers/probers-detail-page.component";

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
                component: GatewaysSummaryPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Gateways",
                    breadcrumbs: []
                }
            },
            {
                path     : "gateway/:id",
                component: GatewaysDetailPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Gateways",
                    breadcrumbs: [
                        {
                            title: "Gateway Summary",
                            url  : "/gateways/summary"
                        }
                    ]
                }
            },
            {
                path     : "gateway/:id/log",
                component: GatewaysDetailPageLogComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Gateways",
                    breadcrumbs: [
                        {
                            title: "Gateway Summary",
                            url  : "/gateways/summary"
                        },
                        {
                            title: "Gateway Details",
                            url  : "/gateways/gateway/:id"
                        }
                    ]
                }
            },
            {
                path     : "gateway/:id/prober",
                component: ProbersDetailPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Prober",
                    breadcrumbs: [
                        {
                            title: "Gateway Summary",
                            url  : "/gateways/summary"
                        },
                        {
                            title: "Gateway Details",
                            url  : "/gateways/gateway/:id"
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
export class GatewaysRoutingModule {}
