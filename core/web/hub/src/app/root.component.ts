import {Component} from "@angular/core";
import {Router} from "@angular/router";
import {AppService} from "app/app.service";
import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import moment from "framework/utils/moment";

@Component({
               selector: "o3-root",
               template: "<router-outlet></router-outlet>"
           })
export class RootComponent
{
    constructor(private appService: AppService,
                private router: Router,
                private cacheService: CacheService,
                private errorService: ErrorService)
    {
        // Fetch the version from the page.
        appService.environment.version = window.OPTIO3_VERSION;

        // Change your page title here
        appService.setState("topnavTitle", "Loading");

        // start a new cache session
        this.cacheService.init();

        // update moment date formats
        moment.updateLocale("en", {
            longDateFormat: {
                LT  : "h:mm A",
                LTS : "h:mm:ss A",
                L   : "MM/DD/YYYY",
                l   : "M/D/YY",
                LL  : "MM/DD/YYYY h:mm A",
                ll  : "M/DD/YY h:mm A",
                LLL : "MM/DD/YYYY h:mm A",
                lll : "M/DD/YY h:mm A",
                LLLL: "MM/DD/YYYY h:mm A",
                llll: "M/DD/YY h:mm A"
            }
        });

    }
}
