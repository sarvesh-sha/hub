import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {DeliveryOptionsComponent} from "app/customer/configuration/common/delivery-options.component";
import {SearchModule} from "app/shared/search/search.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  DeliveryOptionsComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  SearchModule
              ],
              exports     : [
                  DeliveryOptionsComponent
              ]
          })
export class CommonConfigurationModule {}
