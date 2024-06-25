import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {SearchResultsPageComponent} from "app/customer/search/search-results-page.component";
import {AppNavigationResolver} from "app/app-navigation.resolver";

const routes: Routes = [
    {
        path     : "",
        component: SearchResultsPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title           : "Search",
            ignoreAsPrevious: true,
            breadcrumbs     : [
                {
                    title: "Home",
                    url  : "/home"
                }
            ]
        }
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class SearchRoutingModule {}
