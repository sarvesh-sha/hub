import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";

@Component({
               selector   : "o3-threads-page",
               templateUrl: "./threads-page.component.html"
           })
export class ThreadsPageComponent extends SharedSvc.BaseApplicationComponent
{
    threads: string = "<not fetched yet>";

    constructor(inj: Injector)
    {
        super(inj);
    }

    ngAfterViewInit()
    {
        this.loadThreads();
    }

    async loadThreads()
    {
        try
        {
            this.threads = await this.app.domain.apis.adminTasks.dumpThreads();
        }
        catch (e)
        {
            this.threads = "<failed to fetch threads>";
        }
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
