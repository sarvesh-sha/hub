import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {AppNavigationResolver} from "app/app-navigation.resolver";
import {ClassificationDetailPageComponent} from "app/customer/configuration/classification/classification-detail-page.component";
import {ClassificationSummaryPageComponent} from "app/customer/configuration/classification/classification-summary-page.component";

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
                component: ClassificationSummaryPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Classification Versions",
                    breadcrumbs: []
                }
            },
            {
                path     : "item/:id",
                component: ClassificationDetailPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Classification Versions",
                    breadcrumbs: [
                        {
                            title: "Classification Versions",
                            url  : "/classification/summary"
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
export class ClassificationRoutingModule {}
