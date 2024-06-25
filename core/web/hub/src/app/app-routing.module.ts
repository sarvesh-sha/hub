import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {AppComponent} from "app/app.component";
import {BookmarkRedirectComponent} from "app/shared/bookmarks/bookmark-redirect.component";

/**
 * Router Setting
 *
 * Write your component (Page) here to load.
 */
const routes: Routes = [

    // application routes
    {
        path     : "",
        component: AppComponent,
        children : [
            {
                path      : "",
                redirectTo: "home",
                pathMatch : "full"
            },
            {
                path        : "home",
                loadChildren: () => import("app/dashboard/dashboard/dashboard.module").then(m => m.DashboardModule)
            },
            {
                // Redirect to the bookmark's URL if a matching bookmark is found.
                path     : "bookmark/:id",
                component: BookmarkRedirectComponent
            },
            {
                path        : "bookmarks",
                loadChildren: () => import("app/shared/bookmarks/bookmarks.module").then((m) => m.BookmarksModule)
            },
            {
                path        : "search/:area",
                loadChildren: () => import("app/customer/search/search.module").then(m => m.SearchModule)
            },
            {
                path        : "search",
                loadChildren: () => import("app/customer/search/search.module").then(m => m.SearchModule)
            },
            {
                path        : "notification-center",
                loadChildren: () => import("app/dashboard/message-center/message-center.module").then(m => m.MessageCenterModule)
            },
            {
                path        : "user",
                loadChildren: () => import("app/dashboard/user/user.module").then(m => m.UserModule)
            },
            {
                path        : "devices",
                loadChildren: () => import("app/customer/devices/devices.module").then(m => m.DevicesModule)
            },
            {
                path        : "device-elements",
                loadChildren: () => import("app/customer/device-elements/device-elements.module").then(m => m.DeviceElementsModule)
            },
            {
                path        : "alerts",
                loadChildren: () => import("app/customer/alerts/alerts.module").then(m => m.AlertsModule)
            },
            {
                path        : "workflows",
                loadChildren: () => import("app/customer/workflows/workflows.module").then(m => m.WorkflowsModule)
            },
            {
                path        : "gateways",
                loadChildren: () => import("app/customer/data-collection/gateways/gateways.module").then(m => m.GatewaysModule)
            },
            {
                path        : "hosts",
                loadChildren: () => import("app/customer/maintenance/hosts/hosts.module").then(m => m.HostsModule)
            },
            {
                path        : "networks",
                loadChildren: () => import("app/customer/data-collection/networks/networks.module").then(m => m.NetworksModule)
            },
            {
                path        : "sampling",
                loadChildren: () => import("app/customer/data-collection/sampling/sampling.module").then(m => m.SamplingModule)
            },
            {
                path        : "legacy-imports",
                loadChildren: () => import("app/customer/configuration/legacy-imports/legacy-imports.module").then(m => m.LegacyImportsModule)
            },
            {
                path        : "classification",
                loadChildren: () => import("app/customer/configuration/classification/classification.module").then(m => m.ClassificationModule)
            },
            {
                path        : "visualization",
                loadChildren: () => import("app/customer/visualization/visualization.module").then(m => m.VisualizationModule)
            },
            {
                path        : "background-activities",
                loadChildren: () => import("app/customer/maintenance/background-activities/background-activities.module").then(m => m.BackgroundActivitiesModule)
            },
            {
                path        : "equipment",
                loadChildren: () => import("app/customer/equipment/equipment.module").then(m => m.EquipmentModule)
            },
            {
                path        : "configuration",
                loadChildren: () => import("app/customer/configuration/configuration.module").then(m => m.ConfigurationModule)
            },
            {
                path        : "reports",
                loadChildren: () => import("app/reports/reports.module").then(m => m.ReportsModule)
            },
            {
                path        : "experiments",
                loadChildren: () => import("app/dashboard/experiments/experiments.module").then(m => m.ExperimentsModule)
            }
        ]
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class AppRoutingModule {}
