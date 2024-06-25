import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";

import {AppNavigationResolver} from "app/app-navigation.resolver";
import {DashboardPageComponent} from "app/dashboard/dashboard/dashboard-page.component";
import {DashboardToolbarComponent} from "app/dashboard/dashboard/dashboard-toolbar.component";

const routes: Routes = [
    {
        path     : "",
        component: DashboardPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title          : "Home",
            breadcrumbs    : [],
            topnavComponent: DashboardToolbarComponent
        }
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class DashboardRoutingModule {}
