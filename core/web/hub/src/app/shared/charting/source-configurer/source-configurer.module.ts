import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {AddRelatedSourceModule} from "app/shared/charting/add-related-source/add-related-source.module";
import {RangeOverrideModule} from "app/shared/charting/range-override/range-override.module";
import {SourceConfigurerComponent} from "app/shared/charting/source-configurer/source-configurer.component";
import {ColorsModule} from "app/shared/colors/colors.module";
import {EquivalentUnitsSelectorModule} from "app/shared/dropdowns/equivalent-units-selector/equivalent-units-selector.module";
import {TimeRangeModule} from "app/shared/forms/time-range/time-range.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [SourceConfigurerComponent],
              imports: [
                  CommonModule,
                  FrameworkUIModule,
                  ColorsModule,
                  AddRelatedSourceModule,
                  TimeRangeModule,
                  RangeOverrideModule,
                  EquivalentUnitsSelectorModule
              ],
              exports     : [SourceConfigurerComponent]
          })
export class SourceConfigurerModule {}
