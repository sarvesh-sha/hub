import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {AppNavigationResolver} from "app/app-navigation.resolver";
import {WorkflowsDetailPageComponent} from "app/customer/workflows/workflows-detail-page.component";
import {WorkflowsSummaryPageComponent} from "app/customer/workflows/workflows-summary-page.component";

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
                component: WorkflowsSummaryPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Workflows",
                    breadcrumbs: []
                }
            },
            {
                path     : "workflow/:id",
                component: WorkflowsDetailPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Workflows",
                    breadcrumbs: [
                        {
                            title: "Workflows Summary",
                            url  : "/workflows/summary"
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
export class WorkflowsRoutingModule {}
