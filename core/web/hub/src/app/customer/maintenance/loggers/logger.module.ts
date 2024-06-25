import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {LoggersComponent} from "app/customer/maintenance/loggers/loggers.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [LoggersComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [LoggersComponent]
          })
export class LoggerModule {}
