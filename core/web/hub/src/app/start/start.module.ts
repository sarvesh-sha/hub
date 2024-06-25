import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {FormsModule} from "@angular/forms";
import {MatButtonModule} from "@angular/material/button";
import {MatCardModule} from "@angular/material/card";
import {MatInputModule} from "@angular/material/input";
import {ForgotPasswordPageComponent} from "app/start/forgot-password-page.component";
import {LoginPageComponent} from "app/start/login-page.component";
import {ResetPasswordPageComponent} from "app/start/reset-password-page.component";
import {StartRoutingModule} from "app/start/start-routing.module";
import {FrameworkUIModule} from "framework/ui";
import {ErrorListPaneModule} from "framework/ui/errors/error-list-pane.module";

@NgModule({
              imports: [
                  StartRoutingModule,
                  CommonModule,
                  FormsModule,
                  MatCardModule,
                  MatButtonModule,
                  MatInputModule,
                  ErrorListPaneModule,
                  FrameworkUIModule
              ],
              declarations: [
                  LoginPageComponent,
                  ResetPasswordPageComponent,
                  ForgotPasswordPageComponent
              ]
          })
export class StartModule {}
