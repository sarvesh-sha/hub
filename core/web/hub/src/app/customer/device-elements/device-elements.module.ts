import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {DeviceElementsListPageComponent} from "app/customer/device-elements/device-elements-list-page.component";
import {DeviceElementsListModule} from "app/customer/device-elements/device-elements-list.module";
import {DeviceElementsRoutingModule} from "app/customer/device-elements/device-elements-routing.module";
import {BookmarkModule} from "app/shared/bookmarks/bookmark.module";
import {FilterModule} from "app/shared/filter/filter.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [DeviceElementsListPageComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  DeviceElementsListModule,
                  BookmarkModule,
                  DeviceElementsRoutingModule,
                  FilterModule
              ]
          })
export class DeviceElementsModule {}
