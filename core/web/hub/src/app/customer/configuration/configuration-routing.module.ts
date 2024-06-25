import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {DatagramSessionsPageComponent} from "app/customer/maintenance/datagram/datagram-sessions-page.component";
import {LoggersPageComponent} from "app/customer/maintenance/loggers/loggers-page.component";
import {ThreadsPageComponent} from "app/customer/maintenance/threads/threads-page.component";
import {AppNavigationResolver} from "app/app-navigation.resolver";

const routes: Routes = [
    {
        path    : "",
        children: [
            {
                path        : "panes",
                loadChildren: () => import("./panes/panes.module").then((m) => m.PanesModule)
            },
            {
                path        : "asset-structures",
                loadChildren: () => import("./asset-structures/asset-structures.module").then((m) => m.AssetStructuresModule)
            },
            {
                path      : "",
                redirectTo: "users",
                pathMatch : "full"
            },
            {
                path        : "users",
                loadChildren: () => import("./users/users.module").then((m) => m.UsersModule)
            },
            {
                path        : "user-groups",
                loadChildren: () => import("./user-groups/user-groups.module").then((m) => m.UserGroupsModule)
            },
            {
                path        : "reports",
                loadChildren: () => import("./reports/reports.module").then(m => m.ReportsModule)
            },
            {
                path        : "alert-rules",
                loadChildren: () => import("./alert-rules/alert-rules.module").then((m) => m.AlertRulesModule)
            },
            {
                path        : "metrics",
                loadChildren: () => import("./metrics/metrics.module").then((m) => m.MetricsModule)
            },

            {
                path        : "locations",
                loadChildren: () => import("./locations/locations.module").then((m) => m.LocationsModule)
            },
            {
                path     : "loggers",
                component: LoggersPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Loggers",
                    breadcrumbs: []
                }
            },
            {
                path     : "threads",
                component: ThreadsPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Threads",
                    breadcrumbs: []
                }
            },
            {
                path     : "datagram-sessions",
                component: DatagramSessionsPageComponent,
                resolve  : {breadcrumbs: AppNavigationResolver},
                data     : {
                    title      : "Datagram Sessions",
                    breadcrumbs: []
                }
            }
        ]
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class ConfigurationRoutingModule {}
