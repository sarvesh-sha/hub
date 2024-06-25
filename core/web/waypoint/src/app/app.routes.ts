import {Routes} from "@angular/router";
// optio3 customer page components
import {LoggersPageComponent} from "app/customer/maintenance/loggers/loggers-page.component";
import {ThreadsPageComponent} from "app/customer/maintenance/threads/threads-page.component";
import {ProvisionPageComponent} from "app/customer/provision/provision-page.component";
import {SensorsPageComponent} from "app/customer/sensors/sensors-page.component";
// optio3 dashboard page components
import {DashboardPageComponent} from "app/dashboard/dashboard-page.component";
// optio3 error page components
import {ErrorPageComponent} from "app/error/error-page.component";
import {NotFoundPageComponent} from "app/error/not-found-page.component";
// optio3 reports
// domain services
import {AppNavigationResolver} from "app/shared/app-navigation.resolver";
// optio3 start page components

/**
 * Router Setting
 *
 * Write your component (Page) here to load.
 */
export const ROUTES: Routes = [

    // application routes
    {
        path: "",
        children: [
            {
                path: "",
                redirectTo: "home",
                pathMatch: "full"
            },
            {
                path: "home",
                component: DashboardPageComponent,
                resolve: {breadcrumbs: AppNavigationResolver},
                data: {
                    title: "Home",
                    breadcrumbs: []
                }
            },
            {
                path: "sensors",
                component: SensorsPageComponent,
                resolve: {breadcrumbs: AppNavigationResolver},
                data: {
                    title: "Sensors",
                    breadcrumbs: []
                }
            },
            {
                path: "provision",
                component: ProvisionPageComponent,
                resolve: {breadcrumbs: AppNavigationResolver},
                data: {
                    title: "Provision",
                    breadcrumbs: []
                }
            },
            {
                path: "configuration",
                children: [
                    {
                        path: "loggers",
                        component: LoggersPageComponent,
                        resolve: {breadcrumbs: AppNavigationResolver},
                        data: {
                            title: "Settings",
                            breadcrumbs: []
                        }
                    },
                    {
                        path: "threads",
                        component: ThreadsPageComponent,
                        resolve: {breadcrumbs: AppNavigationResolver},
                        data: {
                            title: "Settings",
                            breadcrumbs: []
                        }
                    }
                ]
            }
        ]
    },

    // error pages
    {
        path: "error",
        children: [
            {
                path: "",
                redirectTo: "general",
                pathMatch: "full"
            },
            {
                path: "general",
                component: ErrorPageComponent
            },
            {
                path: "not-found",
                component: NotFoundPageComponent
            }
        ]
    },
    {
        path: "**",
        redirectTo: "error/not-found"
    }

    // Emergency loading, need to import component form file.
    //{
    //  path: 'dashboard',
    //  component: DashboardComponent
    //},
    // Lazy loading, you need to create a module file.
    //
    // 1. Find file dashboard.module.lazy at folder dashboard
    // 2. Rename file dashboard.module.lazy to dashboard.module.ts
    // 3. Modify this file
    //    change Line "component: DashboardComponent" to "loadChildren: './dashboard/dashboard.module#DashboardModule'"
    // 4. Modify file app.module.ts
    //    remove line "DashboardComponent," and "import { DashboardComponent } from './dashboard/dashboard.component';"
    //
    // {
    //   path: 'dashboard',
    //   loadChildren: './dashboard/dashboard.module#DashboardModule'
    // },
];
