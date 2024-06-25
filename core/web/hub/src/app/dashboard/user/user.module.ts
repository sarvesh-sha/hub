import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {UserWizardModule} from "app/customer/configuration/users/wizard/user-wizard.module";
import {UserPasswordChangePageComponent} from "app/dashboard/user/user-password-change-page.component";
import {UserProfilePageComponent} from "app/dashboard/user/user-profile-page.component";
import {UserRoutingModule} from "app/dashboard/user/user-routing.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  UserPasswordChangePageComponent,
                  UserProfilePageComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  UserRoutingModule,
                  UserWizardModule
              ]
          })
export class UserModule {}
