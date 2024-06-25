import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {MetricDefinitionDetailsPageComponent} from "app/customer/configuration/metrics/metric-definition-details-page.component";
import {MetricListPageComponent} from "app/customer/configuration/metrics/metric-list-page.component";
import {AppNavigationResolver} from "app/app-navigation.resolver";

const routes: Routes = [
    {
        path     : "",
        component: MetricListPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "Metrics",
            breadcrumbs: []
        }
    },
    {
        path     : "metric/:id",
        component: MetricDefinitionDetailsPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "Metrics",
            breadcrumbs: [
                {
                    title: "Manage Metrics",
                    url  : "configuration/metrics"
                }
            ]
        }
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class MetricsRoutingModule {}
