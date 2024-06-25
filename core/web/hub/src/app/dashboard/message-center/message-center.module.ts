import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {MessageCenterPageComponent} from "app/dashboard/message-center/message-center-page.component";
import {MessageCenterRoutingModule} from "app/dashboard/message-center/message-center-routing.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [MessageCenterPageComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  MessageCenterRoutingModule
              ]
          })
export class MessageCenterModule {}
