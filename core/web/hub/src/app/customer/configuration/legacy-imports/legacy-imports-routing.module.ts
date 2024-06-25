import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {LegacyImportsDetailPageComponent} from "app/customer/configuration/legacy-imports/legacy-imports-detail-page.component";
import {LegacyImportsSummaryPageComponent} from "app/customer/configuration/legacy-imports/legacy-imports-summary-page.component";
import {AppNavigationResolver} from "app/app-navigation.resolver";

const routes: Routes = [
    {
        path    : "",
        children: [
            {
                path      : "",
                redirectTo: "summary",
                pathMatch : "full"
            },
            {
                path     : "summary",
                component: LegacyImportsSummaryPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Legacy Imports",
                    breadcrumbs: []
                }
            },
            {
                path     : "item/:id",
                component: LegacyImportsDetailPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Legacy Import",
                    breadcrumbs: [
                        {
                            title: "Legacy Imports",
                            url  : "/legacy-imports/summary"
                        }
                    ]
                }
            }
        ]
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class LegacyImportsRoutingModule {}
