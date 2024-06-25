import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {DeviceElementsListComponent} from "app/customer/device-elements/device-elements-list.component";
import {TimeSeriesChartPreviewModule} from "app/shared/assets/chart-preview/time-series-chart-preview.module";
import {PreviewInvokerModule} from "app/shared/utils/preview-invoker/preview-invoker.module";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [DeviceElementsListComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  PreviewInvokerModule,
                  TimeSeriesChartPreviewModule
              ],
              exports     : [DeviceElementsListComponent]
          })
export class DeviceElementsListModule {}
