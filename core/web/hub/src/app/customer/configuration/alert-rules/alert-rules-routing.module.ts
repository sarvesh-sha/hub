import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {AppNavigationResolver} from "app/app-navigation.resolver";
import {AlertRuleDetailsPageComponent} from "app/customer/configuration/alert-rules/alert-rule-details-page.component";
import {AlertRuleListPageComponent} from "app/customer/configuration/alert-rules/alert-rule-list-page.component";

const routes: Routes = [
    {
        path     : "",
        component: AlertRuleListPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "Alert Rules",
            breadcrumbs: []
        }
    },
    {
        path     : "library",
        component: AlertRuleListPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "Alert Rules",
            isLibrary  : true,
            breadcrumbs: [
                {
                    title: "Manage Alert Rules",
                    url  : "configuration/alert-rules"
                }
            ]
        }
    },
    {
        path     : "alert-rule/:id",
        component: AlertRuleDetailsPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "Alert Rules",
            breadcrumbs: [
                {
                    title: "Manage Alert Rules",
                    url  : "configuration/alert-rules"
                }
            ]
        }
    },
    {
        path     : "library/alert-rule/:id",
        component: AlertRuleDetailsPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "Alert Rules",
            isLibrary  : true,
            breadcrumbs: [
                {
                    title: "Manage Alert Rules",
                    url  : "configuration/alert-rules"
                },
                {
                    title: "Library",
                    url  : "configuration/alert-rules/library"
                }
            ]
        }
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class AlertRulesRoutingModule {}
