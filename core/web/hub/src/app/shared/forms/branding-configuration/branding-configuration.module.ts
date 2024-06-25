import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {ColorsModule} from "app/shared/colors/colors.module";
import {BrandingConfigurationComponent} from "app/shared/forms/branding-configuration/branding-configuration.component";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [BrandingConfigurationComponent],
              imports     : [
                  ColorsModule,
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [BrandingConfigurationComponent]
          })
export class BrandingConfigurationModule {}
