import {Component, Injector, ViewChild} from "@angular/core";

import {GatewayExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {convertLogFilters, LogFormatter} from "app/shared/logging/application-log";

import {ApplicationLogFilter, IConsoleLogEntry, IConsoleLogProvider} from "framework/ui/consoles/console-log";
import {ConsoleLogComponent} from "framework/ui/consoles/console-log.component";
import {DownloadDialogComponent, DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";

@Component({
               selector   : "o3-gateways-detail-page-log",
               templateUrl: "./gateways-detail-page-log.component.html"
           })
export class GatewaysDetailPageLogComponent extends SharedSvc.BaseComponentWithRouter implements IConsoleLogProvider
{
    gatewayId: string;

    gateway: GatewayExtended;

    @ViewChild("log", {static: true}) log: ConsoleLogComponent;

    logLockScroll: boolean;
    private logPreviousGateway: GatewayExtended;
    private scrollToEnd: boolean;

    constructor(inj: Injector)
    {
        super(inj);

        this.scrollToEnd = true;
        this.gateway     = this.app.domain.assets.wrapTypedModel(GatewayExtended, new Models.GatewayAsset());
    }

    ngAfterViewInit()
    {
        super.ngAfterViewInit();

        this.log.bind(this);
    }

    protected async onNavigationComplete()
    {
        this.gatewayId = this.getPathParameter("id");

        this.loadGateway();
    }

    async loadGateway()
    {
        if (this.gatewayId)
        {
            // load gateway info
            let gateway = await this.app.domain.assets.getTypedExtendedById(GatewayExtended, this.gatewayId);
            if (!gateway)
            {
                this.exit();
                return;
            }

            this.gateway = gateway;

            if (!this.logPreviousGateway || this.logPreviousGateway.model.sysId != this.gatewayId)
            {
                this.log.reset();
                this.logPreviousGateway = gateway;
                this.scrollToEnd        = true;
            }

            // set breadcrumbs
            let model                                     = this.gateway.typedModel;
            this.app.ui.navigation.breadcrumbCurrentLabel = model.name || model.instanceId;

            //--//

            this.removeAllDbSubscriptions();

            this.subscribeOneShot(gateway,
                                  async (ext,
                                         action) =>
                                  {
                                      this.loadGateway();
                                  });

            this.logLockScroll = true;
            this.log.refresh(this.scrollToEnd);
            this.scrollToEnd = false;
        }
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    async truncate(days?: number)
    {
        if (this.gateway)
        {
            let minutes: number;

            if (days !== undefined)
            {
                if (!await this.confirmOperation(`Click Yes to confirm deletion of log entries older than ${days} days.`))
                {
                    return;
                }

                minutes = days * 24 * 60;
            }
            else
            {
                if (!await this.confirmOperation(`Click Yes to confirm deletion of all log entries.`))
                {
                    return;
                }

                minutes = null;
            }

            await this.gateway.deleteLog(minutes);

            this.log.reset();
            this.loadGateway();
        }
    }

    //--//

    getLogCount(): number
    {
        return this.gateway ? this.gateway.typedModel.lastOffset : 0;
    }

    async getLogPage(start: number,
                     end: number): Promise<IConsoleLogEntry[]>
    {
        let logEntries = [];

        let lines = await this.gateway.getLog(start, end, null) || [];
        for (let line of lines)
        {
            logEntries.push(this.log.newLogEntry(line));
        }

        return logEntries;
    }

    async performFilter(filters: ApplicationLogFilter)
    {
        return await this.gateway.filterLog(convertLogFilters(filters)) || [];
    }

    async prepareDownload()
    {
        if (this.gateway)
        {
            let file = this.gateway.model.name;
            if (!file) file = this.gateway.typedModel.instanceId;

            DownloadDialogComponent.openWithGenerator(this, "Gateway Log", DownloadDialogComponent.fileName(file, ".txt"), new GatewayLogDownloader(this.gateway));
        }
    }
}

class GatewayLogDownloader implements DownloadGenerator
{
    logEntries: Models.LogLine[] = [];
    lastOffset: number           = 0;

    constructor(private extended: GatewayExtended)
    {
    }

    public getProgressPercent()
    {
        return NaN;
    }

    public getProgressMessage()
    {
        return "Lines in log: " + this.logEntries.length;
    }

    public async makeProgress(dialog: DownloadDialogComponent): Promise<boolean>
    {
        let lines = await this.extended.getLog(this.lastOffset, null, 4357); // Random-looking number, better for the UI, less flickering
        if (!lines || lines.length == 0)
        {
            return true;
        }

        this.logEntries.push(...lines);

        let lastLine    = lines[lines.length - 1];
        this.lastOffset = lastLine.lineNumber + 1;

        return false;
    }

    public async sleepForProgress(): Promise<void>
    {
        // We don't need to sleep.
    }

    public isDeterminate()
    {
        return false;
    }

    public async getResults(): Promise<DownloadResults>
    {
        return {lines: LogFormatter.formatLines(this.logEntries)};
    }
}
