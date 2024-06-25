import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {AlertsDetailPageComponent} from "app/customer/alerts/alerts-detail-page.component";
import {AlertsListComponent} from "app/customer/alerts/alerts-list.component";
import {AlertsRoutingModule} from "app/customer/alerts/alerts-routing.module";
import {AlertsSummaryPageComponent} from "app/customer/alerts/alerts-summary-page.component";
import {DevicesDetailModule} from "app/customer/devices/devices-details-page/devices-detail.module";
import {SelectorModule} from "app/shared/dropdowns/selector.module";
import {FilterModule} from "app/shared/filter/filter.module";
import {TimelineModule} from "app/shared/timelines/timeline.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  AlertsDetailPageComponent,
                  AlertsListComponent,
                  AlertsSummaryPageComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  DevicesDetailModule,
                  TimelineModule,
                  FilterModule,
                  SelectorModule,
                  AlertsRoutingModule
              ]
          })
export class AlertsModule {}
