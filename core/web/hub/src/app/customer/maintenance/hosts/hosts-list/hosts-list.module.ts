import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {HostsListComponent} from "app/customer/maintenance/hosts/hosts-list/hosts-list.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [HostsListComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [HostsListComponent]
          })
export class HostsListModule {}
