import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {AlertIconComponent} from "app/shared/timelines/alert-icon.component";
import {TimelineComponent} from "app/shared/timelines/timeline.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  AlertIconComponent,
                  TimelineComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [
                  AlertIconComponent,
                  TimelineComponent
              ]
          })
export class TimelineModule {}
