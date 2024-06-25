import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {DeviceElementsListModule} from "app/customer/device-elements/device-elements-list.module";
import {EquipmentDetailPageComponent} from "app/customer/equipment/equipment-detail-page.component";
import {EquipmentListModule} from "app/customer/equipment/equipment-list.module";
import {EquipmentRoutingModule} from "app/customer/equipment/equipment-routing.module";
import {EquipmentSummaryPageComponent} from "app/customer/equipment/equipment-summary-page.component";
import {WorkflowWizardModule} from "app/customer/workflows/wizard/workflow-wizard.module";
import {BookmarkModule} from "app/shared/bookmarks/bookmark.module";
import {SelectorModule} from "app/shared/dropdowns/selector.module";
import {FilterModule} from "app/shared/filter/filter.module";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  EquipmentDetailPageComponent,
                  EquipmentSummaryPageComponent
              ],
              imports: [
                  CommonModule,
                  FrameworkUIModule,
                  WorkflowWizardModule,
                  DeviceElementsListModule,
                  EquipmentListModule,
                  EquipmentRoutingModule,
                  FilterModule,
                  SelectorModule,
                  BookmarkModule
              ]
          })
export class EquipmentModule {}
