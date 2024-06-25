import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {AlertSeverityConfigurerComponent} from "app/shared/alerts/alert-severity/alert-severity-configurer.component";
import {ColorsModule} from "app/shared/colors/colors.module";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [AlertSeverityConfigurerComponent],
              imports     : [
                  ColorsModule,
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [AlertSeverityConfigurerComponent]
          })
export class AlertSeverityModule {}
