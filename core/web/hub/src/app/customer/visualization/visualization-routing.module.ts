import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {DataExplorerPageComponent} from "app/customer/visualization/data-explorer-page.component";
import {AppNavigationResolver} from "app/app-navigation.resolver";

const routes: Routes = [
    {
        path    : "",
        children: [
            {
                path      : "",
                redirectTo: "explorer",
                pathMatch : "full"
            },
            {
                path     : "explorer",
                component: DataExplorerPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Data Explorer",
                    breadcrumbs: []
                }
            }
        ]
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class VisualizationRoutingModule {}
