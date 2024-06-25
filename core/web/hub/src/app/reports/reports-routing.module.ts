import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {ReportContainerComponent} from "app/reports/report-container.component";
import {ReportGridViewComponent} from "app/reports/report-grid-view.component";
import {AppNavigationResolver} from "app/app-navigation.resolver";

const routes: Routes = [
    {
        path    : "",
        children: [
            {
                path     : "",
                component: ReportContainerComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Reports",
                    breadcrumbs: []
                }
            },
            {
                path     : "grid",
                component: ReportGridViewComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Reports - Printable Grid",
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
export class ReportsRoutingModule {}
