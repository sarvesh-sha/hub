import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {GatewaySelectionDialogComponent} from "app/customer/data-collection/gateways/gateway-selection-dialog.component";
import {GatewaysDetailPageLogComponent} from "app/customer/data-collection/gateways/gateways-detail-page-log.component";
import {GatewaysDetailPageComponent} from "app/customer/data-collection/gateways/gateways-detail-page.component";
import {GatewaysListComponent} from "app/customer/data-collection/gateways/gateways-list.component";
import {GatewaysRoutingModule} from "app/customer/data-collection/gateways/gateways-routing.module";
import {GatewaysSummaryPageComponent} from "app/customer/data-collection/gateways/gateways-summary-page.component";
import {ProbersBDTListComponent} from "app/customer/data-collection/gateways/probers/probers-bdt-list.component";
import {ProbersDetailPageDialogComponent} from "app/customer/data-collection/gateways/probers/probers-detail-page-dialog.component";
import {ProbersDetailPageComponent} from "app/customer/data-collection/gateways/probers/probers-detail-page.component";
import {ProbersDeviceListComponent} from "app/customer/data-collection/gateways/probers/probers-device-list.component";
import {ProbersFDTListComponent} from "app/customer/data-collection/gateways/probers/probers-fdt-list.component";
import {ProbersFrameListComponent} from "app/customer/data-collection/gateways/probers/probers-frame-list.component";
import {ProbersObjectListComponent} from "app/customer/data-collection/gateways/probers/probers-object-list.component";
import {ProbersWizardActionStep} from "app/customer/data-collection/gateways/probers/probers-wizard-action.step";
import {ProbersWizardBacnetConfigStep} from "app/customer/data-collection/gateways/probers/probers-wizard-bacnet-config-step.component";
import {ProbersWizardBacnetOpStep} from "app/customer/data-collection/gateways/probers/probers-wizard-bacnet-op.step";
import {ProbersWizardCanbusConfigStep} from "app/customer/data-collection/gateways/probers/probers-wizard-canbus-config-step.component";
import {ProbersWizardCanbusOpStep} from "app/customer/data-collection/gateways/probers/probers-wizard-canbus-op-step.component";
import {ProbersWizardIpnConfigStep} from "app/customer/data-collection/gateways/probers/probers-wizard-ipn-config-step.component";
import {ProbersWizardIpnOpStep} from "app/customer/data-collection/gateways/probers/probers-wizard-ipn-op-step.component";
import {ProbersWizardNewDeviceStep} from "app/customer/data-collection/gateways/probers/probers-wizard-new-device.step";
import {ProbersWizardProtocolStep} from "app/customer/data-collection/gateways/probers/probers-wizard-protocol.step";
import {NetworksListModule} from "app/customer/data-collection/networks/networks-list/networks-list.module";
import {LoggerModule} from "app/customer/maintenance/loggers/logger.module";
import {TimeSeriesChartModule} from "app/shared/charting/time-series-chart/time-series-chart.module";
import {SelectorModule} from "app/shared/dropdowns/selector.module";
import {FilterModule} from "app/shared/filter/filter.module";
import {TimeRangeModule} from "app/shared/forms/time-range/time-range.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  GatewaysListComponent,
                  GatewaysSummaryPageComponent,
                  GatewaysDetailPageComponent,
                  GatewaysDetailPageLogComponent,
                  GatewaySelectionDialogComponent,
                  ProbersDetailPageComponent,
                  ProbersDetailPageDialogComponent,
                  ProbersBDTListComponent,
                  ProbersFDTListComponent,
                  ProbersDeviceListComponent,
                  ProbersObjectListComponent,
                  ProbersFrameListComponent,
                  ProbersWizardActionStep,
                  ProbersWizardBacnetConfigStep,
                  ProbersWizardBacnetOpStep,
                  ProbersWizardCanbusConfigStep,
                  ProbersWizardCanbusOpStep,
                  ProbersWizardIpnConfigStep,
                  ProbersWizardIpnOpStep,
                  ProbersWizardNewDeviceStep,
                  ProbersWizardProtocolStep
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  LoggerModule,
                  GatewaysRoutingModule,
                  TimeRangeModule,
                  TimeSeriesChartModule,
                  FilterModule,
                  SelectorModule,
                  NetworksListModule
              ]
          })
export class GatewaysModule {}
