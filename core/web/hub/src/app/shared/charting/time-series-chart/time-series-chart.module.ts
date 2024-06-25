import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {AnnotationModule} from "app/shared/charting/annotations/annotation.module";
import {RangeOverrideModule} from "app/shared/charting/range-override/range-override.module";
import {SourceChipModule} from "app/shared/charting/source-chip/source-chip.module";
import {SourceConfigurerModule} from "app/shared/charting/source-configurer/source-configurer.module";
import {TimeSeriesChartConfigurationComponent} from "app/shared/charting/time-series-chart/time-series-chart-configuration.component";
import {TimeSeriesChartSourcesConfigurationComponent} from "app/shared/charting/time-series-chart/time-series-chart-sources-configuration.component";
import {TimeSeriesChartComponent} from "app/shared/charting/time-series-chart/time-series-chart.component";
import {EquivalentUnitsSelectorModule} from "app/shared/dropdowns/equivalent-units-selector/equivalent-units-selector.module";
import {TimeRangeModule} from "app/shared/forms/time-range/time-range.module";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  TimeSeriesChartComponent,
                  TimeSeriesChartConfigurationComponent,
                  TimeSeriesChartSourcesConfigurationComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  AnnotationModule,
                  SourceConfigurerModule,
                  EquivalentUnitsSelectorModule,
                  TimeRangeModule,
                  RangeOverrideModule,
                  SourceChipModule
              ],
              exports     : [TimeSeriesChartComponent]
          })
export class TimeSeriesChartModule {}
