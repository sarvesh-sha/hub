import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {AppNavigationResolver} from "app/app-navigation.resolver";
import {UserDetailPageComponent} from "app/customer/configuration/users/user-detail-page.component";
import {UserListPageComponent} from "app/customer/configuration/users/user-list-page.component";

const routes: Routes = [
    {
        path     : "",
        component: UserListPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "Users",
            breadcrumbs: []
        }
    },
    {
        path     : "user/:id",
        component: UserDetailPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "Users",
            breadcrumbs: [
                {
                    title: "User Management",
                    url  : "configuration/users"
                }
            ]
        }
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class UsersRoutingModule {}
