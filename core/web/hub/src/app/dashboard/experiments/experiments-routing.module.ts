import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";

import {AppNavigationResolver} from "app/app-navigation.resolver";
import {ExperimentsColorPickerPageComponent} from "app/dashboard/experiments/exp-color-picker-page.component";
import {ExperimentsComponentsPageComponent} from "app/dashboard/experiments/exp-components-page.component";
import {ExperimentsContextPanePageComponent} from "app/dashboard/experiments/exp-context-pane-page.component";
import {ExperimentsControlPointSearchPageComponent} from "app/dashboard/experiments/exp-control-point-search-page.component";
import {ExperimentsFormsPageComponent} from "app/dashboard/experiments/exp-forms-page.component";
import {ExperimentsIconsPageComponent} from "app/dashboard/experiments/exp-icons-page.component";
import {ExperimentsMessagesPageComponent} from "app/dashboard/experiments/exp-messages-page.component";

const routes: Routes = [
    {
        path    : "",
        children: [
            {
                path     : "color-picker",
                component: ExperimentsColorPickerPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title           : "Experiments",
                    ignoreAsPrevious: true,
                    breadcrumbs     : []
                }
            },
            {
                path     : "context-pane",
                component: ExperimentsContextPanePageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title           : "Experiments",
                    ignoreAsPrevious: true,
                    breadcrumbs     : []
                }
            },
            {
                path     : "control-point-search",
                component: ExperimentsControlPointSearchPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title           : "Experiments",
                    ignoreAsPrevious: true,
                    breadcrumbs     : []
                }
            },
            {
                path     : "forms",
                component: ExperimentsFormsPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title           : "Experiments",
                    ignoreAsPrevious: true,
                    breadcrumbs     : []
                }
            },
            {
                path     : "icons",
                component: ExperimentsIconsPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title           : "Experiments",
                    ignoreAsPrevious: true,
                    breadcrumbs     : []
                }
            },
            {
                path     : "messages",
                component: ExperimentsMessagesPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title           : "Experiments",
                    ignoreAsPrevious: true,
                    breadcrumbs     : []
                }
            },
            {
                path     : "components",
                component: ExperimentsComponentsPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title           : "Experiments",
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
export class ExperimentsRoutingModule {}
