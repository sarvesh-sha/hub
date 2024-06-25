import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {ControlPointPreviewComponent} from "app/shared/assets/chart-preview/control-point-preview.component";
import {ControlPointSummaryComponent} from "app/shared/assets/chart-preview/control-point-summary.component";
import {DataSourceWizardModule} from "app/shared/charting/data-source-wizard/data-source-wizard.module";
import {TimeSeriesChartModule} from "app/shared/charting/time-series-chart/time-series-chart.module";
import {TimeRangeModule} from "app/shared/forms/time-range/time-range.module";
import {GpsMapModule} from "app/shared/mapping/gps-map/gps-map.module";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  ControlPointPreviewComponent,
                  ControlPointSummaryComponent
              ],
              imports     : [
                  CommonModule,
                  DataSourceWizardModule,
                  FrameworkUIModule,
                  GpsMapModule,
                  TimeSeriesChartModule,
                  TimeRangeModule
              ],
              exports     : [
                  ControlPointPreviewComponent
              ]
          })
export class TimeSeriesChartPreviewModule {}
