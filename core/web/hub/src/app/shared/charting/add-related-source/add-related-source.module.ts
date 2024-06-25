import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {AddRelatedSourceComponent} from "app/shared/charting/add-related-source/add-related-source.component";
import {TimeRangeModule} from "app/shared/forms/time-range/time-range.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [AddRelatedSourceComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  TimeRangeModule
              ],
              exports     : [AddRelatedSourceComponent]
          })
export class AddRelatedSourceModule {}
