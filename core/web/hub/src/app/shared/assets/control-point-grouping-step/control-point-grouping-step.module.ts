import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {ScheduleSelectorOverlayModule} from "app/dashboard/overlays/schedule-selector-overlay.module";
import {AssetGraphSelectorModule} from "app/shared/assets/asset-graph-selectors/asset-graph-selector.module";
import {ControlPointGroupingStepComponent} from "app/shared/assets/control-point-grouping-step/control-point-grouping-step.component";
import {ControlPointsGroupConfigurerComponent} from "app/shared/assets/control-point-grouping-step/control-points-group-configurer.component";
import {DataSourceWizardModule} from "app/shared/charting/data-source-wizard/data-source-wizard.module";
import {RangeOverrideModule} from "app/shared/charting/range-override/range-override.module";
import {ColorsModule} from "app/shared/colors/colors.module";
import {EquivalentUnitsSelectorModule} from "app/shared/dropdowns/equivalent-units-selector/equivalent-units-selector.module";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  ControlPointGroupingStepComponent,
                  ControlPointsGroupConfigurerComponent
              ],
              imports     : [
                  AssetGraphSelectorModule,
                  ColorsModule,
                  CommonModule,
                  DataSourceWizardModule,
                  EquivalentUnitsSelectorModule,
                  FrameworkUIModule,
                  RangeOverrideModule,
                  ScheduleSelectorOverlayModule
              ],
              exports     : [ControlPointGroupingStepComponent]
          })
export class ControlPointGroupingStepModule {}
