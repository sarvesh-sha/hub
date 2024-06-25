import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {AppNavigationResolver} from "app/app-navigation.resolver";
import {PaneDetailsPageComponent} from "app/customer/configuration/panes/pane-details-page.component";
import {PaneListPageComponent} from "app/customer/configuration/panes/pane-list-page.component";

const routes: Routes = [
    {
        path     : "",
        component: PaneListPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title: "Panes"
        }
    },
    {
        path     : ":id",
        component: PaneDetailsPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "Panes",
            breadcrumbs: [
                {
                    title: "Panes",
                    url  : "configuration/panes"
                }
            ]
        }
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class PanesRoutingModule {}
