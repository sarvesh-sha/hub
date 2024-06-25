import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {SelectorModule} from "app/shared/dropdowns/selector.module";
import {DeviceElementFiltersAdapterComponent} from "app/shared/filter/asset/device-element-filters-adapter.component";
import {DeviceFiltersAdapterComponent} from "app/shared/filter/asset/device-filters-adapter.component";
import {EquipmentFiltersAdapterComponent} from "app/shared/filter/asset/equipment-filters-adapter.component";
import {GatewayFiltersAdapterComponent} from "app/shared/filter/asset/gateway-filters-adapter.component";
import {HostFiltersAdapterComponent} from "app/shared/filter/asset/host-filters-adapter.component";
import {NetworkFiltersAdapterComponent} from "app/shared/filter/asset/network-filters-adapter.component";
import {AlertFiltersAdapterComponent} from "app/shared/filter/event/alert-filters-adapter.component";
import {WorkflowFiltersAdapterComponent} from "app/shared/filter/event/workflow-filters-adapter.component";
import {FilterOverlayComponent} from "app/shared/filter/filter-overlay.component";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  AlertFiltersAdapterComponent,
                  DeviceElementFiltersAdapterComponent,
                  DeviceFiltersAdapterComponent,
                  EquipmentFiltersAdapterComponent,
                  FilterOverlayComponent,
                  GatewayFiltersAdapterComponent,
                  HostFiltersAdapterComponent,
                  NetworkFiltersAdapterComponent,
                  WorkflowFiltersAdapterComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  SelectorModule
              ],
              exports     : [
                  AlertFiltersAdapterComponent,
                  DeviceElementFiltersAdapterComponent,
                  DeviceFiltersAdapterComponent,
                  EquipmentFiltersAdapterComponent,
                  FilterOverlayComponent,
                  GatewayFiltersAdapterComponent,
                  HostFiltersAdapterComponent,
                  NetworkFiltersAdapterComponent,
                  WorkflowFiltersAdapterComponent
              ]
          })
export class FilterModule {}
