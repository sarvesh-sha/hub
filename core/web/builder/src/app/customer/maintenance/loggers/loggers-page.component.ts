import {Component, Injector, ViewChild} from "@angular/core";
import {ReportError} from "app/app.service";
import {LoggersComponent} from "app/customer/maintenance/loggers/loggers.component";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-loggers-page",
               templateUrl: "./loggers-page.component.html"
           })
export class LoggersPageComponent extends SharedSvc.BaseApplicationComponent
{
    loggers: Array<Models.LoggerConfiguration> = [];

    @ViewChild("loggersComp", { static: true })loggersComp: LoggersComponent;

    constructor(inj: Injector)
    {
        super(inj);
    }

    ngAfterViewInit()
    {
        this.loadLoggers();
    }

    async loadLoggers()
    {
        this.loggers = await this.app.domain.apis.adminTasks.getLoggers();
        this.detectChanges();
    }

    @ReportError
    async save()
    {
        for (let logger of this.loggers)
        {
            if (this.loggersComp.wasUpdated(logger))
            {
                await this.app.domain.apis.adminTasks.configLogger(logger);
            }
        }

        this.loadLoggers();
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
