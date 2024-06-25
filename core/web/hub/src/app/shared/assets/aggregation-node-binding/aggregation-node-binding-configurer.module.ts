import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {AggregationNodeBindingConfigurerComponent} from "app/shared/assets/aggregation-node-binding/aggregation-node-binding-configurer.component";
import {AssetGraphSelectorModule} from "app/shared/assets/asset-graph-selectors/asset-graph-selector.module";
import {RangeOverrideModule} from "app/shared/charting/range-override/range-override.module";
import {ColorsModule} from "app/shared/colors/colors.module";
import {EquivalentUnitsSelectorModule} from "app/shared/dropdowns/equivalent-units-selector/equivalent-units-selector.module";

import {FrameworkUIModule} from "framework/ui";
import {SortModule} from "framework/ui/shared/sort/sort.module";

@NgModule({
              declarations: [AggregationNodeBindingConfigurerComponent],
              imports     : [
                  AssetGraphSelectorModule,
                  ColorsModule,
                  CommonModule,
                  EquivalentUnitsSelectorModule,
                  FrameworkUIModule,
                  RangeOverrideModule,
                  SortModule
              ],
              exports     : [AggregationNodeBindingConfigurerComponent]
          })
export class AggregationNodeBindingConfigurerModule {}
