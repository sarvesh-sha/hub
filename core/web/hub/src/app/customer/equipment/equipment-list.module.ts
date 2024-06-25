import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {EquipmentListComponent} from "app/customer/equipment/equipment-list.component";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [EquipmentListComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [EquipmentListComponent]
          })
export class EquipmentListModule {}
