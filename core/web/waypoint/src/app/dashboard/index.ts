import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {FormsModule} from "@angular/forms";

import {DashboardPageComponent} from "app/dashboard/dashboard-page.component";
import {SharedModule} from "app/shared";
import {CdkModule} from "framework/cdk";

import {MaterialModule} from "framework/material";
import {FrameworkServicesModule} from "framework/services";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  DashboardPageComponent
              ],
              imports     : [
                  CommonModule,
                  FormsModule,
                  MaterialModule,
                  FrameworkServicesModule,
                  FrameworkUIModule,
                  SharedModule,
                  CdkModule
              ],
              exports     : []
          })
export class DashboardModule
{
}
