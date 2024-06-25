import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {DevicesListModule} from "app/customer/devices/devices-list.module";
import {HostsDetailPageLogComponent} from "app/customer/maintenance/hosts/hosts-detail-page-log.component";
import {HostsDetailPageComponent} from "app/customer/maintenance/hosts/hosts-detail-page.component";
import {HostsListModule} from "app/customer/maintenance/hosts/hosts-list/hosts-list.module";
import {HostsRoutingModule} from "app/customer/maintenance/hosts/hosts-routing.module";
import {HostsSummaryPageComponent} from "app/customer/maintenance/hosts/hosts-summary-page.component";
import {TimeSeriesChartModule} from "app/shared/charting/time-series-chart/time-series-chart.module";
import {SelectorModule} from "app/shared/dropdowns/selector.module";
import {FilterModule} from "app/shared/filter/filter.module";
import {TimeRangeModule} from "app/shared/forms/time-range/time-range.module";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  HostsSummaryPageComponent,
                  HostsDetailPageComponent,
                  HostsDetailPageLogComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  DevicesListModule,
                  HostsListModule,
                  SelectorModule,
                  FilterModule,
                  TimeSeriesChartModule,
                  TimeRangeModule,
                  HostsRoutingModule
              ]
          })
export class HostsModule {}
