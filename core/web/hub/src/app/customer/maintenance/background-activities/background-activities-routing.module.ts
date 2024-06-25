import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {BackgroundActivitiesSummaryPageComponent} from "app/customer/maintenance/background-activities/background-activities-summary-page.component";
import {BackgroundActivityDetailPageComponent} from "app/customer/maintenance/background-activities/background-activity-detail-page.component";
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
                component: BackgroundActivitiesSummaryPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Background Activities",
                    breadcrumbs: []
                }
            },
            {
                path     : "item/:id",
                component: BackgroundActivityDetailPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Background Activities",
                    breadcrumbs: [
                        {
                            title: "Background Activities",
                            url  : "/background-activities/summary"
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
export class BackgroundActivitiesRoutingModule {}
