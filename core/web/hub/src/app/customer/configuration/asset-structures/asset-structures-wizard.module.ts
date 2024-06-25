import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {AssetStructureWizardDataStepComponent} from "app/customer/configuration/asset-structures/wizard/asset-structure-wizard-data-step.component";
import {AssetStructureWizardDialogComponent} from "app/customer/configuration/asset-structures/wizard/asset-structure-wizard-dialog.component";
import {AssetStructureWizardNameStepComponent} from "app/customer/configuration/asset-structures/wizard/asset-structure-wizard-name-step.component";
import {AssetGraphStepModule} from "app/shared/assets/asset-graph-step/asset-graph-step.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  AssetStructureWizardDialogComponent,
                  AssetStructureWizardDataStepComponent,
                  AssetStructureWizardNameStepComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  AssetGraphStepModule
              ],
              exports     : [
                  AssetStructureWizardDialogComponent
              ]
          })
export class AssetStructuresWizardModule {}
