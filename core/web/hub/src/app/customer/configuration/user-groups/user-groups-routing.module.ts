import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {AppNavigationResolver} from "app/app-navigation.resolver";
import {UserGroupDetailPageComponent} from "app/customer/configuration/user-groups/user-group-detail-page.component";
import {UserGroupListPageComponent} from "app/customer/configuration/user-groups/user-group-list-page.component";

const routes: Routes = [
    {
        path     : "",
        component: UserGroupListPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "User Groups",
            breadcrumbs: []
        }
    },
    {
        path     : "user-group/:id",
        component: UserGroupDetailPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "User Groups",
            breadcrumbs: [
                {
                    title: "Group Management",
                    url  : "configuration/user-groups"
                }
            ]
        }
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class UserGroupsRoutingModule {}
