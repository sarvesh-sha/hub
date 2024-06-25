import {NgModule} from "@angular/core";
import {NoPreloading, RouterModule, Routes} from "@angular/router";

import {AuthGuard} from "app/services/domain/auth.guard";

const routes: Routes = [
    {
        path      : "",
        pathMatch : "full",
        redirectTo: "home"
    },
    {
        path        : "start",
        canLoad     : [AuthGuard],
        loadChildren: () => import("./start/start.module").then(m => m.StartModule)
    },
    {
        path        : "error",
        loadChildren: () => import("./error/error.module").then(m => m.ErrorModule)
    },
    {
        path        : "tests",
        loadChildren: () => import("./test/test.module").then(m => m.TestModule)
    },
    {
        path            : "",
        canLoad         : [AuthGuard],
        canActivateChild: [AuthGuard],
        loadChildren    : () => import("./app.module").then(m => m.AppModule)
    },
    {
        path      : "**",
        redirectTo: "error/not-found"
    }
];

@NgModule({
              imports: [
                  RouterModule.forRoot(routes, {
                      useHash           : true,
                      preloadingStrategy: NoPreloading
                  })
              ],
              exports: [RouterModule]
          })
export class RootRoutingModule {}
