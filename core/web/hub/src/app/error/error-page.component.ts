import {Component, OnDestroy, OnInit} from "@angular/core";

import {AppService} from "app/app.service";

@Component({
               selector   : "o3-error-page",
               templateUrl: "./error-page.component.html",
               styleUrls  : ["./error-page.component.scss"]
           })
export class ErrorPageComponent implements OnInit,
                                           OnDestroy
{

    constructor(private appService: AppService)
    {
        appService.setState("topnavTitle", "Error");
        appService.setState("pageFullscreen", true);
        appService.setState("startPage", true);
    }

    ngOnInit()
    {
    }

    ngOnDestroy()
    {
        this.appService.setState("pageFullscreen", false);
        this.appService.setState("startPage", false);
    }
}
