import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {FormsModule} from "@angular/forms";
import {DashboardManagementService} from "app/dashboard/dashboard-management.service";

import {DashboardPageComponent} from "app/dashboard/dashboard-page.component";
import {UserPasswordChangePageComponent} from "app/dashboard/user-password-change-page.component";
import {UserProfilePageComponent} from "app/dashboard/user-profile-page.component";
import {SharedModule} from "app/shared";
import {CdkModule} from "framework/cdk";

import {MaterialModule} from "framework/material";
import {FrameworkServicesModule} from "framework/services";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  DashboardPageComponent,
                  UserProfilePageComponent,
                  UserPasswordChangePageComponent
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
              providers      : [
                  DashboardManagementService
              ],
              exports     : []
          })
export class DashboardModule {}
