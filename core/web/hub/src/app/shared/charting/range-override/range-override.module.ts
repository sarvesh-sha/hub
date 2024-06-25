import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {RangeOverrideComponent} from "app/shared/charting/range-override/range-override.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [RangeOverrideComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
    exports: [RangeOverrideComponent]
          })
export class RangeOverrideModule {}
