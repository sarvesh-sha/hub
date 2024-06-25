import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";

import {AppNavigationResolver} from "app/app-navigation.resolver";
import {BookmarksPageComponent} from "app/shared/bookmarks/bookmarks-page.component";

const routes: Routes = [
    {
        path     : "",
        component: BookmarksPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "Bookmarks",
            breadcrumbs: []
        }
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class BookmarksRoutingModule {}
