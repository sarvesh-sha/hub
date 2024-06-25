import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {TimeSeriesContainerModule} from "app/shared/charting/time-series-container/time-series-container.module";
import {TimeSeriesSetToolbarActionComponent, TimeSeriesSetToolbarComponent} from "app/shared/charting/time-series-set/time-series-set-toolbar.component";
import {TimeSeriesSetComponent} from "app/shared/charting/time-series-set/time-series-set.component";
import {TimeRangeModule} from "app/shared/forms/time-range/time-range.module";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  TimeSeriesSetComponent,
                  TimeSeriesSetToolbarComponent,
                  TimeSeriesSetToolbarActionComponent,
                  TimeSeriesSetToolbarComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  TimeRangeModule,
                  TimeSeriesContainerModule
              ],
              exports     : [
                  TimeSeriesSetComponent,
                  TimeSeriesSetToolbarComponent,
                  TimeSeriesSetToolbarActionComponent,
                  TimeSeriesSetToolbarComponent
              ]
          })
export class TimeSeriesSetModule {}
