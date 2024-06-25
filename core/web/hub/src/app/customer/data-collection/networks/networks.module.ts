import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {BACnetBBMDTableComponent, BACnetSubnetTableComponent, I2C_MCP3428_TableComponent, I2C_SHT30x_TableComponent, NetworkDetailListEditorComponent, NonDiscoverableDeviceTableComponent, NonDiscoverableMSTPTableComponent, SkippedDeviceTableComponent} from "app/customer/data-collection/networks/network-detail-editor.component";
import {NetworksDetailPageComponent} from "app/customer/data-collection/networks/networks-detail-page.component";
import {NetworksListModule} from "app/customer/data-collection/networks/networks-list/networks-list.module";
import {NetworksRoutingModule} from "app/customer/data-collection/networks/networks-routing.module";
import {NetworksSummaryPageComponent} from "app/customer/data-collection/networks/networks-summary-page.component";
import {NetworksWizardBacnetStepComponent} from "app/customer/data-collection/networks/networks-wizard/networks-wizard-bacnet-step.component";
import {NetworksWizardConfirmStepComponent} from "app/customer/data-collection/networks/networks-wizard/networks-wizard-confirm-step.component";
import {NetworksWizardDetailsStepComponent} from "app/customer/data-collection/networks/networks-wizard/networks-wizard-details-step.component";
import {NetworksWizardIpnStepComponent} from "app/customer/data-collection/networks/networks-wizard/networks-wizard-ipn-step.component";
import {NetworksWizardTypeStepComponent} from "app/customer/data-collection/networks/networks-wizard/networks-wizard-type-step.component";
import {NetworksWizardDialogComponent} from "app/customer/data-collection/networks/networks-wizard/networks-wizard.component";
import {DevicesListModule} from "app/customer/devices/devices-list.module";
import {TimeSeriesChartModule} from "app/shared/charting/time-series-chart/time-series-chart.module";
import {SelectorModule} from "app/shared/dropdowns/selector.module";
import {FilterModule} from "app/shared/filter/filter.module";
import {TimeRangeModule} from "app/shared/forms/time-range/time-range.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  NetworksSummaryPageComponent,
                  NetworksDetailPageComponent,
                  NetworksWizardDialogComponent,
                  NetworksWizardDetailsStepComponent,
                  NetworksWizardTypeStepComponent,
                  NetworksWizardBacnetStepComponent,
                  NetworksWizardIpnStepComponent,
                  NetworksWizardConfirmStepComponent,
                  NetworkDetailListEditorComponent,
                  NonDiscoverableDeviceTableComponent,
                  NonDiscoverableMSTPTableComponent,
                  BACnetBBMDTableComponent,
                  BACnetSubnetTableComponent,
                  I2C_MCP3428_TableComponent,
                  I2C_SHT30x_TableComponent,
                  SkippedDeviceTableComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  DevicesListModule,
                  NetworksListModule,
                  SelectorModule,
                  FilterModule,
                  TimeSeriesChartModule,
                  TimeRangeModule,
                  NetworksRoutingModule
              ]
          })
export class NetworksModule {}
