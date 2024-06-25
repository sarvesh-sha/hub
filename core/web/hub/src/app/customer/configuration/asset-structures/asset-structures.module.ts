import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {AssetStructureDetailsPageComponent} from "app/customer/configuration/asset-structures/asset-structure-details-page.component";
import {AssetStructureListPageComponent} from "app/customer/configuration/asset-structures/asset-structure-list-page.component";
import {AssetStructuresRoutingModule} from "app/customer/configuration/asset-structures/asset-structures-routing.module";
import {AssetStructuresWizardModule} from "app/customer/configuration/asset-structures/asset-structures-wizard.module";
import {AssetGraphStepModule} from "app/shared/assets/asset-graph-step/asset-graph-step.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  AssetStructureDetailsPageComponent,
                  AssetStructureListPageComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  AssetGraphStepModule,
                  AssetStructuresWizardModule,
                  AssetStructuresRoutingModule
              ]
          })
export class AssetStructuresModule {}
