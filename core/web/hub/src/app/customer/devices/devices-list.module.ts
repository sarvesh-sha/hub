import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {DevicesListComponent} from "app/customer/devices/devices-list.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [DevicesListComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [DevicesListComponent]
          })
export class DevicesListModule {}
