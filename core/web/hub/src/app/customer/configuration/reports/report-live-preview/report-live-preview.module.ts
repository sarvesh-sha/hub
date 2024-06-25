import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {ReportLivePreviewComponent} from "app/customer/configuration/reports/report-live-preview/report-live-preview.component";
import {ReportModule} from "app/reports/report.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [ReportLivePreviewComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  ReportModule
              ],
              exports     : [ReportLivePreviewComponent]
          })
export class ReportLivePreviewModule {}
