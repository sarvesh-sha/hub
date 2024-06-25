﻿import {Component, Injector, ViewChild} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {HostExtended} from "app/services/domain/hosts.service";
import * as Models from "app/services/proxy/model/models";

import {convertLogFilters} from "app/shared/logging/application-log";

import {ApplicationLogFilter, IApplicationLogRange, IConsoleLogEntry, IConsoleLogProvider} from "framework/ui/consoles/console-log";
import {ConsoleLogComponent} from "framework/ui/consoles/console-log.component";
import {DownloadDialogComponent, DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

@Component({
               selector   : "o3-hosts-detail-page-log",
               templateUrl: "./hosts-detail-page-log.component.html"
           })
export class HostsDetailPageLogComponent extends SharedSvc.BaseComponentWithRouter implements IConsoleLogProvider
{
    id: string;
    ext: HostExtended;

    @ViewChild("log", {static: true}) log: ConsoleLogComponent;

    logLockScroll: boolean;
    private logPrevious: HostExtended;
    private scrollToEnd: boolean;

    constructor(inj: Injector)
    {
        super(inj);

        this.scrollToEnd = true;
        this.ext         = this.app.domain.hosts.wrapModel(new Models.Host());
    }

    ngAfterViewInit()
    {
        super.ngAfterViewInit();

        this.log.bind(this);
    }

    protected async onNavigationComplete()
    {
        this.id = this.getPathParameter("id");

        this.loadHost();
    }

    async loadHost()
    {
        if (this.id)
        {
            // load host info
            let host = await this.app.domain.hosts.getExtendedById(this.id);
            if (!host)
            {
                this.exit();
                return;
            }

            this.ext = host;

            if (!this.logPrevious || this.logPrevious.model.sysId != this.id)
            {
                this.log.reset();
                this.logPrevious = host;
                this.scrollToEnd = true;
            }

            // set breadcrumbs
            let model                                     = this.ext.model;
            this.app.ui.navigation.breadcrumbCurrentLabel = model.domainName;

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
        if (this.ext)
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

            await this.ext.deleteLog(minutes);

            this.log.reset();
            this.loadHost();
        }
    }

    //--//

    getLogCount(): number
    {
        return this.ext ? this.ext.model.lastOffset : 0;
    }

    async getLogPage(start: number,
                     end: number): Promise<IConsoleLogEntry[]>
    {
        let logEntries = [];

        let lines = await this.ext.getLog(start, end, null) || [];
        for (let line of lines)
        {
            logEntries.push(this.log.newLogEntry(line));
        }

        return logEntries;
    }

    async performFilter(filters: ApplicationLogFilter): Promise<IApplicationLogRange[]>
    {
        return await this.ext.filterLog(convertLogFilters(filters)) || [];
    }

    prepareDownload()
    {
        if (this.ext)
        {
            let file = this.ext.model.domainName;
            if (!file) file = this.ext.model.sysId;

            DownloadDialogComponent.openWithGenerator(this, "Host Log", DownloadDialogComponent.fileName(file, ".txt"), new LogDownloader(this.ext));
        }
    }
}

class LogDownloader implements DownloadGenerator
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
        let len_level    = 0;
        let len_thread   = 0;
        let len_selector = 0;

        for (let line of this.logEntries)
        {
            len_level    = Math.max(len_level, line.level?.length);
            len_thread   = Math.max(len_thread, line.thread?.length);
            len_selector = Math.max(len_selector, line.selector?.length);
        }

        let res: string[] = [];

        for (let line of this.logEntries)
        {
            let timestamp = MomentHelper.parse(line.timestamp);
            let log       = `${timestamp.format("YYYY-MM-DD HH:mm:ss.SSS")}: `;

            log = this.append(log, line.level.toString(), len_level);
            log = this.append(log, line.thread, len_thread);
            log = this.append(log, line.selector, len_selector);
            log += line.line.replace("\n", "");

            res.push(log);
        }

        return {lines: res};
    }

    private append(line: string,
                   text: string,
                   len: number)
    {
        if (!text) text = "";

        return `${line}${text.padEnd(len)} | `;
    }
}
