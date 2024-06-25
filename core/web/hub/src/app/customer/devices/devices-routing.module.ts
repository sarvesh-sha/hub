import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {AppNavigationResolver} from "app/app-navigation.resolver";
import {DeviceElementsDetailPageComponent} from "app/customer/device-elements/device-elements-detail-page.component";
import {DevicesDetailPageComponent} from "app/customer/devices/devices-details-page/devices-detail-page.component";
import {DevicesSummaryPageComponent} from "app/customer/devices/devices-summary-page.component";

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
                component: DevicesSummaryPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Devices",
                    breadcrumbs: []
                }
            },
            {
                path     : "device/:id",
                component: DevicesDetailPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Devices",
                    breadcrumbs: [
                        {
                            title: "Device Summary",
                            url  : "/devices/summary"
                        }
                    ]
                }
            },
            {
                path     : "device/:id/element/:elementId",
                component: DeviceElementsDetailPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Devices",
                    breadcrumbs: [
                        {
                            title: "Device Summary",
                            url  : "/devices/summary"
                        },
                        {
                            title: "Devices",
                            url  : "/devices/device/:id"
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
export class DevicesRoutingModule {}
