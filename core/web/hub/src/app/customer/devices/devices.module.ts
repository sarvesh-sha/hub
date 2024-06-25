import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {DeviceElementsDetailPageComponent} from "app/customer/device-elements/device-elements-detail-page.component";
import {DeviceElementsListModule} from "app/customer/device-elements/device-elements-list.module";
import {DevicesDetailModule} from "app/customer/devices/devices-details-page/devices-detail.module";
import {DevicesListModule} from "app/customer/devices/devices-list.module";
import {DevicesRoutingModule} from "app/customer/devices/devices-routing.module";
import {DevicesSummaryPageComponent} from "app/customer/devices/devices-summary-page.component";
import {WorkflowWizardModule} from "app/customer/workflows/wizard/workflow-wizard.module";
import {BookmarkModule} from "app/shared/bookmarks/bookmark.module";
import {EquivalentUnitsSelectorModule} from "app/shared/dropdowns/equivalent-units-selector/equivalent-units-selector.module";
import {SelectorModule} from "app/shared/dropdowns/selector.module";
import {FilterModule} from "app/shared/filter/filter.module";
import {TimelineModule} from "app/shared/timelines/timeline.module";
import {ClipboardModule} from "app/shared/utils/clipboard/clipboard.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  DevicesSummaryPageComponent,
                  DeviceElementsDetailPageComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  DevicesDetailModule,
                  BookmarkModule,
                  DevicesListModule,
                  DeviceElementsListModule,
                  WorkflowWizardModule,
                  SelectorModule,
                  EquivalentUnitsSelectorModule,
                  ClipboardModule,
                  FilterModule,
                  TimelineModule,
                  DevicesRoutingModule
              ]
          })
export class DevicesModule {}
