import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {DurationSelectorComponent} from "app/shared/forms/time-range/duration-selector.component";
import {RangeSelectorComponent} from "app/shared/forms/time-range/range-selector.component";
import {ScheduleSelectorComponent} from "app/shared/forms/time-range/schedule-selector.component";
import {TimeZoneSelectorComponent} from "app/shared/forms/time-range/time-zone-selector.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  DurationSelectorComponent,
                  RangeSelectorComponent,
                  ScheduleSelectorComponent,
                  TimeZoneSelectorComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [
                  DurationSelectorComponent,
                  RangeSelectorComponent,
                  ScheduleSelectorComponent,
                  TimeZoneSelectorComponent
              ]
          })
export class TimeRangeModule {}
