import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {ReportDefinitionDetailsPageComponent} from "app/customer/configuration/reports/report-definition-details-page.component";
import {ReportWizardModule} from "app/customer/configuration/reports/wizard/report-wizard.module";
import {ReportHistoryDetailsPageComponent} from "app/customer/configuration/reports/report-history-details-page.component";
import {ReportHistoryListComponent} from "app/customer/configuration/reports/report-history-list.component";
import {ReportViewDialogComponent} from "app/customer/configuration/reports/report-view-dialog.component";
import {ReportListPageComponent} from "app/customer/configuration/reports/report-list-page.component";
import {ReportsRoutingModule} from "app/customer/configuration/reports/reports-routing.module";
import {FilterModule} from "app/shared/filter/filter.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  ReportDefinitionDetailsPageComponent,
                  ReportHistoryListComponent,
                  ReportHistoryDetailsPageComponent,
                  ReportListPageComponent,
                  ReportViewDialogComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  ReportWizardModule,
                  ReportsRoutingModule,
                  FilterModule
              ]
          })
export class ReportsModule {}
