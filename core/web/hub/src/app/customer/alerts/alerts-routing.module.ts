import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {AlertsDetailPageComponent} from "app/customer/alerts/alerts-detail-page.component";
import {AlertsSummaryPageComponent} from "app/customer/alerts/alerts-summary-page.component";
import {DevicesDetailPageComponent} from "app/customer/devices/devices-details-page/devices-detail-page.component";
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
                component: AlertsSummaryPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Alerts",
                    breadcrumbs: []
                }
            },
            {
                path     : "alert/:id",
                component: AlertsDetailPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Alerts",
                    breadcrumbs: [
                        {
                            title: "Alert Summary",
                            url  : "/alerts/summary"
                        }
                    ]
                }
            },
            {
                path     : "alert/:alertID/device/:id",
                component: DevicesDetailPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Alerts",
                    breadcrumbs: [
                        {
                            title: "Alert Summary",
                            url  : "/alerts/summary"
                        },
                        {
                            title: "Alert Details",
                            url  : "/alerts/alert/:alertID"
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
export class AlertsRoutingModule {}
