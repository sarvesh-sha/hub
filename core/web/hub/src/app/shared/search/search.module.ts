import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {ControlPointSelectorComponent} from "app/shared/search/control-point-selector.component";
import {DeviceElementSearchFiltersComponent} from "app/shared/search/device-element-search-filters.component";
import {EquipmentSearchFiltersComponent} from "app/shared/search/equipment-search-filters.component";
import {EquipmentSelectorComponent} from "app/shared/search/equipment-selector.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  ControlPointSelectorComponent,
                  DeviceElementSearchFiltersComponent,
                  EquipmentSelectorComponent,
                  EquipmentSearchFiltersComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [
                  ControlPointSelectorComponent,
                  DeviceElementSearchFiltersComponent,
                  EquipmentSelectorComponent,
                  EquipmentSearchFiltersComponent
              ]
          })
export class SearchModule {}
