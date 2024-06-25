import {Component, Input, OnInit, ViewEncapsulation} from "@angular/core";
import {Router} from "@angular/router";

import {AppService} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {LoggingService} from "framework/services/logging.service";

import {Future} from "framework/utils/concurrency";
import moment from "framework/utils/moment";

@Component({
               selector     : "o3-app",
               templateUrl  : "./app.component.html",
               encapsulation: ViewEncapsulation.None
           })
export class AppComponent implements OnInit
{
    @Input() version: string;
    private appVersion: string;

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

        // set error service behavior
        this.errorService.autoDismiss = true;

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
        this.validateAppVersion();
    }

    private async validateAppVersion()
    {
        let api = this.appService.inject(ApiService);

        if (await api.adminTasks.isProductionMode())
        {
            this.appService.setState("pageFullscreen", true);
            this.appService.routing.router.navigate(["/provision"]);
        }

        while (true)
        {
            try
            {
                let appVersion = await api.adminTasks.getAppVersion();
                if (this.appVersion && this.appVersion != appVersion)
                {
                    console.log("###### RELOADING ######");
                    let url = window.location.href;
                    let pos = url.indexOf("/#/");
                    window.location.assign(url.substr(0, pos));
                }
                this.appVersion = appVersion;
            }
            catch (e)
            {
                //console.log(`Error due to ${e}`);
            }

            await Future.delayed(15000);
        }
    }
}
