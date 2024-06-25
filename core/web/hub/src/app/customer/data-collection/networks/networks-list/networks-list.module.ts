import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {NetworksListComponent} from "app/customer/data-collection/networks/networks-list/networks-list.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [NetworksListComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [NetworksListComponent]
          })
export class NetworksListModule {}
