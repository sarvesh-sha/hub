import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {AssetGraphSelectorModule} from "app/shared/assets/asset-graph-selectors/asset-graph-selector.module";
import {AssetGraphStepModule} from "app/shared/assets/asset-graph-step/asset-graph-step.module";
import {AssetGraphBindingsWizardStep} from "app/shared/assets/asset-graph-wizard/asset-graph-bindings-wizard-step.component";
import {AssetGraphConfigureWizardStep} from "app/shared/assets/asset-graph-wizard/asset-graph-configure-wizard-step.component";
import {AssetGraphWizardComponent} from "app/shared/assets/asset-graph-wizard/asset-graph-wizard.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  AssetGraphWizardComponent,
                  AssetGraphConfigureWizardStep,
                  AssetGraphBindingsWizardStep
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  AssetGraphStepModule,
                  AssetGraphSelectorModule
              ],
              exports     : [AssetGraphWizardComponent]
          })
export class AssetGraphWizardModule {}
