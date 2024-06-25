import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {AuthGuard} from "app/services/domain/auth.guard";
import {ForgotPasswordPageComponent} from "app/start/forgot-password-page.component";
import {LoginPageComponent} from "app/start/login-page.component";
import {ResetPasswordPageComponent} from "app/start/reset-password-page.component";

const routes: Routes = [
    {
        path            : "",
        canActivateChild: [AuthGuard],
        children        : [
            {
                path      : "",
                redirectTo: "login",
                pathMatch : "full"
            },
            {
                path     : "login",
                component: LoginPageComponent
            },
            {
                path     : "forgotpassword",
                component: ForgotPasswordPageComponent
            },
            {
                path     : "resetpassword",
                component: ResetPasswordPageComponent
            }
        ]
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class StartRoutingModule {}
