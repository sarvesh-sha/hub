import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {DynamicReportComponent} from "app/reports/dynamic/dynamic-report.component";
import {ReportContainerComponent} from "app/reports/report-container.component";
import {ReportGridViewComponent} from "app/reports/report-grid-view.component";
import {ReportElementModule} from "app/shared/reports/elements/report-element.module";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  DynamicReportComponent,
                  ReportContainerComponent,
                  ReportGridViewComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  ReportElementModule
              ],
              exports     : [ReportContainerComponent]
          })
export class ReportModule {}
