import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";

import {EquipmentDetailPageComponent} from "app/customer/equipment/equipment-detail-page.component";
import {EquipmentSummaryPageComponent} from "app/customer/equipment/equipment-summary-page.component";
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
                component: EquipmentSummaryPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Equipment",
                    breadcrumbs: []
                }
            },
            {
                path     : "equipment/:id",
                component: EquipmentDetailPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Equipment",
                    breadcrumbs: [
                        {
                            title: "Equipment",
                            url  : "/equipment/summary"
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
export class EquipmentRoutingModule {}
