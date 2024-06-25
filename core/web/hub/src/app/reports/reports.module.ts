import {NgModule} from "@angular/core";
import {ReportModule} from "app/reports/report.module";
import {ReportsRoutingModule} from "app/reports/reports-routing.module";

@NgModule({
              imports: [
                  ReportModule,
                  ReportsRoutingModule
              ]
          })
export class ReportsModule {}
