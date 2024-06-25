import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {ChartSetComponent} from "app/shared/charting/chart-set.component";
import {TimeSeriesSetModule} from "app/shared/charting/time-series-set/time-series-set.module";

@NgModule({
              declarations: [ChartSetComponent],
              imports     : [
                  CommonModule,
                  TimeSeriesSetModule
              ],
              exports     : [ChartSetComponent]
          })
export class ChartingModule {}
