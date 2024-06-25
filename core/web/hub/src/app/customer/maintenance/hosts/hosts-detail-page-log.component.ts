import {Component, Injector, ViewChild} from "@angular/core";

import {HostExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {convertLogFilters, LogFormatter} from "app/shared/logging/application-log";
import {ApplicationLogFilter, IConsoleLogEntry, IConsoleLogProvider} from "framework/ui/consoles/console-log";
import {ConsoleLogComponent} from "framework/ui/consoles/console-log.component";

import {DownloadDialogComponent, DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";

@Component({
               selector   : "o3-hosts-detail-page-log",
               templateUrl: "./hosts-detail-page-log.component.html"
           })
export class HostsDetailPageLogComponent extends SharedSvc.BaseComponentWithRouter implements IConsoleLogProvider
{
    hostId: string;

    host: HostExtended;

    @ViewChild("log", {static: true}) log: ConsoleLogComponent;

    logLockScroll: boolean;
    private logPreviousHost: HostExtended;
    private scrollToEnd: boolean;

    constructor(inj: Injector)
    {
        super(inj);

        this.scrollToEnd = true;
        this.host        = this.app.domain.assets.wrapTypedModel(HostExtended, new Models.HostAsset());
    }

    ngAfterViewInit()
    {
        super.ngAfterViewInit();

        this.log.bind(this);
    }

    protected async onNavigationComplete()
    {
        this.hostId = this.getPathParameter("id");

        this.loadHost();
    }

    async loadHost()
    {
        if (this.hostId)
        {
            // load host info
            let host = await this.app.domain.assets.getTypedExtendedById(HostExtended, this.hostId);
            if (!host)
            {
                this.exit();
                return;
            }

            this.host = host;

            if (!this.logPreviousHost || this.logPreviousHost.model.sysId != this.hostId)
            {
                this.log.reset();
                this.logPreviousHost = host;
                this.scrollToEnd     = true;
            }

            // set breadcrumbs
            let model                                     = this.host.typedModel;
            this.app.ui.navigation.breadcrumbCurrentLabel = model.name;

            //--//

            this.removeAllDbSubscriptions();

            this.subscribeOneShot(host,
                                  async (ext,
                                         action) =>
                                  {
                                      this.loadHost();
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
        if (this.host)
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

            await this.host.deleteLog(minutes);

            this.log.reset();
            this.loadHost();
        }
    }

    //--//

    getLogCount(): number
    {
        return this.host ? this.host.typedModel.lastOffset : 0;
    }

    async getLogPage(start: number,
                     end: number): Promise<IConsoleLogEntry[]>
    {
        let logEntries = [];

        let lines = await this.host.getLog(start, end, null) || [];
        for (let line of lines)
        {
            logEntries.push(this.log.newLogEntry(line));
        }

        return logEntries;
    }

    async performFilter(filters: ApplicationLogFilter)
    {
        return await this.host.filterLog(convertLogFilters(filters)) || [];
    }

    async prepareDownload()
    {
        if (this.host)
        {
            let file = this.host.model.name;
            if (!file) file = this.host.typedModel.sysId;

            DownloadDialogComponent.openWithGenerator(this, "Host Log", DownloadDialogComponent.fileName(file, ".txt"), new HostLogDownloader(this.host));
        }
    }
}

class HostLogDownloader implements DownloadGenerator
{
    logEntries: Models.LogLine[] = [];
    lastOffset: number           = 0;

    constructor(private extended: HostExtended)
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
