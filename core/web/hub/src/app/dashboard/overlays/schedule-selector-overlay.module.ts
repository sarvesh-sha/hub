import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {ScheduleSelectorOverlayComponent} from "app/dashboard/overlays/schedule-selector-overlay.component";
import {TimeRangeModule} from "app/shared/forms/time-range/time-range.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [ScheduleSelectorOverlayComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  TimeRangeModule
              ],
              exports     : [ScheduleSelectorOverlayComponent]
          })
export class ScheduleSelectorOverlayModule {}
