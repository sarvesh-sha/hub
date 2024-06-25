import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {SamplingDetailPageComponent} from "app/customer/data-collection/sampling/sampling-detail-page.component";
import {AppNavigationResolver} from "app/app-navigation.resolver";

const routes: Routes = [ {
    path     : "",
    component: SamplingDetailPageComponent,
    resolve  : {breadcrumbs: AppNavigationResolver},
    data     : {
        title      : "Sampling",
        breadcrumbs: []
    }
}];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class SamplingRoutingModule {}
