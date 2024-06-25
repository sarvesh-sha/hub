import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {ErrorPageComponent} from "app/error/error-page.component";
import {NotFoundPageComponent} from "app/error/not-found-page.component";

const routes: Routes = [
    {
        path    : "",
        children: [
            {
                path      : "",
                redirectTo: "general",
                pathMatch : "full"
            },
            {
                path     : "general",
                component: ErrorPageComponent
            },
            {
                path     : "not-found",
                component: NotFoundPageComponent
            }
        ]
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class ErrorRoutingModule {}
