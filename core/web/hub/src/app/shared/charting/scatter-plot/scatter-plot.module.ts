import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {RangeOverrideModule} from "app/shared/charting/range-override/range-override.module";
import {ScatterPlotConfigurationComponent} from "app/shared/charting/scatter-plot/scatter-plot-configuration.component";
import {ScatterPlotContainerComponent} from "app/shared/charting/scatter-plot/scatter-plot-container.component";
import {ColorsModule} from "app/shared/colors/colors.module";
import {EquivalentUnitsSelectorModule} from "app/shared/dropdowns/equivalent-units-selector/equivalent-units-selector.module";
import {TimeRangeModule} from "app/shared/forms/time-range/time-range.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  ScatterPlotConfigurationComponent,
                  ScatterPlotContainerComponent
              ],
              imports: [
                  CommonModule,
                  FrameworkUIModule,
                  ColorsModule,
                  TimeRangeModule,
                  RangeOverrideModule,
                  EquivalentUnitsSelectorModule
              ],
              exports     : [ScatterPlotContainerComponent]
          })
export class ScatterPlotModule {}
