import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {AppNavigationResolver} from "app/app-navigation.resolver";
import {ReportDefinitionDetailsPageComponent} from "app/customer/configuration/reports/report-definition-details-page.component";
import {ReportHistoryDetailsPageComponent} from "app/customer/configuration/reports/report-history-details-page.component";
import {ReportListPageComponent} from "app/customer/configuration/reports/report-list-page.component";
import {ReportWizardDialogComponent} from "app/customer/configuration/reports/wizard/report-wizard-dialog.component";

const routes: Routes = [
    {
        path     : "",
        component: ReportListPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "Reports",
            breadcrumbs: []
        }
    },
    {
        path     : "configure/:id",
        component: ReportWizardDialogComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "Reports",
            breadcrumbs: [
                {
                    title: "Manage Reports",
                    url  : "configuration/reports"
                }
            ]
        }
    },
    {
        path     : "report/:id/history/:historyID",
        component: ReportHistoryDetailsPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "Reports",
            breadcrumbs: [
                {
                    title: "Manage Reports",
                    url  : "configuration/reports"
                },
                {
                    title: "Report Details",
                    url  : "/configuration/reports/report/:id"
                }
            ]
        }
    },
    {
        path     : "report/:id",
        component: ReportDefinitionDetailsPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "Reports",
            breadcrumbs: [
                {
                    title: "Manage Reports",
                    url  : "configuration/reports"
                }
            ]
        }
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class ReportsRoutingModule {}
