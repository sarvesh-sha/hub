import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {FormsModule} from "@angular/forms";

import {DomainModule} from "app/services/domain";
import {SharedModule} from "app/shared";
import {ForgotPasswordPageComponent} from "app/start/forgot-password-page.component";
import {LoginPageComponent} from "app/start/login-page.component";
import {ResetPasswordPageComponent} from "app/start/reset-password-page.component";

import {MaterialModule} from "framework/material";
import {FrameworkServicesModule} from "framework/services";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  LoginPageComponent,
                  ForgotPasswordPageComponent,
                  ResetPasswordPageComponent
              ],
              imports     : [
                  CommonModule,
                  FormsModule,
                  MaterialModule,
                  FrameworkServicesModule,
                  FrameworkUIModule,
                  DomainModule,
                  SharedModule
              ]
          })
export class StartModule {}
