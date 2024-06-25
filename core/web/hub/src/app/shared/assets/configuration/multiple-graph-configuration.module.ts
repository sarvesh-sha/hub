import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {AssetStructuresWizardModule} from "app/customer/configuration/asset-structures/asset-structures-wizard.module";
import {MultipleGraphConfigurationComponent} from "app/shared/assets/configuration/multiple-graph-configuration.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [MultipleGraphConfigurationComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  AssetStructuresWizardModule
              ],
              exports     : [MultipleGraphConfigurationComponent]
          })
export class MultipleGraphConfigurationModule {}
