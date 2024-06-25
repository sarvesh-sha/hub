import {Component, Input, OnInit, ViewEncapsulation} from "@angular/core";
import {Router} from "@angular/router";

import {AppService} from "app/app.service";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {LoggingService} from "framework/services/logging.service";
import moment from "framework/utils/moment";

@Component({
               selector     : "o3-app",
               templateUrl  : "./app.component.html",
               encapsulation: ViewEncapsulation.None
           })
export class AppComponent implements OnInit
{
    @Input() version: string;

    isStandardLayout: boolean;
    isReportingLayout: boolean;

    constructor(private appService: AppService,
                private router: Router,
                private cacheService: CacheService,
                private errorService: ErrorService,
                private loggingService: LoggingService)
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

    ngOnInit()
    {
        // if the url contains ";sys_asreport=true", use reporting layout
        // (Note the "bad practice" of directly using the window object. Since this is an isolated case of a component that could not
        // be re-used outside the web, it was determined that for simplicity, to just assume availability of this global variable. Further
        // there is no way to access the URL using Ng services/providers this early in the lifecycle)
        if (window?.location.href.indexOf(";sys_asreport=true") > 0)
        {
            this.isReportingLayout = true;
        }
        else if (window?.location.href.indexOf(";sys_forprint=true") > 0)
        {
            this.isReportingLayout = true;
        }
        else
        {
            this.isStandardLayout = true;
        }
    }
}
