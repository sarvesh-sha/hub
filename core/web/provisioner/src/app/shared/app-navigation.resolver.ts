import {Injectable} from "@angular/core";
import {Title} from "@angular/platform-browser";
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot} from "@angular/router";

import {AppService} from "app/app.service";

import {AppNavigationService, BreadcrumbItem} from "framework/ui/navigation/app-navigation.service";

@Injectable()
export class AppNavigationResolver implements Resolve<any>
{

    constructor(private appService: AppService,
                private navigationService: AppNavigationService,
                private titleService: Title)
    {
        // no implementation
    }

    /**
     * Resolve breadcrumbs based on those defined in the routing config.
     * @param route
     * @param state
     */
    resolve(route: ActivatedRouteSnapshot,
            state: RouterStateSnapshot): any
    {

        // set page title
        let title = "";
        if (route.data && route.data["title"])
        {
            title = route.data["title"];
            this.appService.setState("topnavTitle", title);

            title += " | ";
        }
        title += "Optio3 - IoT Under Control";
        this.titleService.setTitle(title);

        // determine if we should ignore as a prevous route
        let ignoreAsPrevious: boolean = false;
        if (route.data && route.data["ignoreAsPrevious"])
        {
            ignoreAsPrevious = route.data["ignoreAsPrevious"] as boolean;
        }

        // enable the reporting capture view
        if (route.params && route.params["sys_asreport"] == "true")
        {
            this.appService.setState("reportingCapture", true);
        }

        // enable the print capture
        if (route.params && route.params["sys_forprint"] == "true")
        {
            this.appService.setState("printTrigger", true);
        }

        // read breadcrumbs from route data
        this.navigationService.breadcrumbCurrentLabel = null;
        this.navigationService.breadcrumbs            = [];
        if (route.data && route.data["breadcrumbs"])
        {

            // create a copy of the breadcrumbs from the route data
            // purpose: a copy is needed to ensure that the route data value isn't changed during the view lifecycle - as
            // can happen with dynamically added breadcrumbs - particularily when a view component links to itself
            // with different params (ie parent to child locations)
            let breadcrumbs = <BreadcrumbItem[]>JSON.parse(JSON.stringify(route.data["breadcrumbs"]));
            if (breadcrumbs && breadcrumbs.length > 0)
            {
                this.navigationService.setBreadcrumbs(breadcrumbs, route.params, ignoreAsPrevious);
                return breadcrumbs;
            }
        }

        // or assume none
        this.navigationService.setBreadcrumbs([], route.params, ignoreAsPrevious);
        return [];
    }
}
