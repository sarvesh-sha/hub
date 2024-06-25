import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {UserPasswordChangePageComponent} from "app/dashboard/user/user-password-change-page.component";
import {UserProfilePageComponent} from "app/dashboard/user/user-profile-page.component";
import {AppNavigationResolver} from "app/app-navigation.resolver";

const routes: Routes = [
    {
        path    : "",
        children: [
            {
                path      : "",
                redirectTo: "profile",
                pathMatch : "full"
            },
            {
                path     : "profile",
                component: UserProfilePageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title           : "User",
                    ignoreAsPrevious: true,
                    breadcrumbs     : []
                }
            },
            {
                path     : "change-password",
                component: UserPasswordChangePageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title           : "User",
                    ignoreAsPrevious: true,
                    breadcrumbs     : []
                }
            }
        ]
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class UserRoutingModule {}
