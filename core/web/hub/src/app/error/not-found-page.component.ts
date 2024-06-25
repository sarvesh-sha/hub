import {Component, OnDestroy, OnInit} from "@angular/core";

import {AppService} from "app/app.service";

@Component({
               selector   : "o3-not-found-page",
               templateUrl: "./not-found-page.component.html",
               styleUrls  : ["./not-found-page.component.scss"]
           })
export class NotFoundPageComponent implements OnInit,
                                              OnDestroy
{

    constructor(private appService: AppService)
    {
        appService.setState("topnavTitle", "Not Found");
        appService.setState("startPage", true);
        appService.setState("pageFullscreen", true);
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
