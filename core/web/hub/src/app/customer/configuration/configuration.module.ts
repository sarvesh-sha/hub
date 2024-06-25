import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {ConfigurationRoutingModule} from "app/customer/configuration/configuration-routing.module";
import {DatagramSessionsPageComponent} from "app/customer/maintenance/datagram/datagram-sessions-page.component";
import {LoggerModule} from "app/customer/maintenance/loggers/logger.module";
import {LoggersPageComponent} from "app/customer/maintenance/loggers/loggers-page.component";
import {ThreadsPageComponent} from "app/customer/maintenance/threads/threads-page.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  DatagramSessionsPageComponent,
                  LoggersPageComponent,
                  ThreadsPageComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  LoggerModule,
                  ConfigurationRoutingModule
              ]
          })
export class ConfigurationModule {}
