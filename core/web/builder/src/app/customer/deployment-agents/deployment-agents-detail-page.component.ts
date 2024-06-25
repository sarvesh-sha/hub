import {Component, Injector, ViewChild} from "@angular/core";
import {SafeHtml} from "@angular/platform-browser";

import {ReportError} from "app/app.service";
import {LoggersComponent} from "app/customer/maintenance/loggers/loggers.component";
import {DashboardManagementService} from "app/dashboard/dashboard-management.service";

import * as SharedSvc from "app/services/domain/base.service";
import {DeploymentAgentExtended} from "app/services/domain/deployment-agents.service";
import {RegistryTaggedImageExtended} from "app/services/domain/registry-tagged-images.service";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {AnsiRenderer, ApplicationLogFilter, IApplicationLogRange, IConsoleLogEntry, IConsoleLogProvider, LogColumn} from "framework/ui/consoles/console-log";
import {ConsoleLogCommandEventArgs, ConsoleLogComponent} from "framework/ui/consoles/console-log.component";
import {DownloadDialogComponent, DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {TabGroupComponent} from "framework/ui/tab-group/tab-group.component";

@Component({
               selector   : "o3-deployment-agents-detail-page",
               templateUrl: "./deployment-agents-detail-page.component.html"
           })
export class DeploymentAgentsDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    id: string;
    extended: DeploymentAgentExtended;

    extendedRemoveChecks: Models.ValidationResult[];
    extendedNoRemoveReason: string;

    extendedTerminateChecks: Models.ValidationResult[];
    extendedNoTerminateReason: string;

    imageTag: string;
    image: RegistryTaggedImageExtended;

    //--//

    threads: string = "<not fetched yet>";
    threadsFetching = false;

    //--//

    loggers: Array<Models.LoggerConfiguration>;
    loggersUpdating: boolean;

    @ViewChild("loggersComp") loggersComp: LoggersComponent;

    //--//

    @ViewChild("log", {static: true}) log: ConsoleLogComponent;

    @ViewChild("tabGroup", {static: true}) tabGroup: TabGroupComponent;

    lockScroll: boolean = true;

    cmd: string;
    shellToken: Models.ShellToken;
    shellTimer: any;
    shellOpening: boolean;

    shellState: ShellState;

    shouldAutologin: boolean;

    constructor(inj: Injector)
    {
        super(inj);

        this.extended = this.app.domain.deploymentAgents.allocateInstance();
    }

    ngAfterViewInit()
    {
        super.ngAfterViewInit();

        this.shellState = new ShellState((html) => { return this.bypassSecurityTrustHtml(html); }, this.log);

        this.log.bind(this.shellState);
    }

    async ngOnDestroy()
    {
        super.ngOnDestroy();

        await this.closeShell();
    }

    protected async onNavigationComplete()
    {
        this.id = this.getPathParameter("id");

        this.shouldAutologin = !!this.getPathParameter("login");

        this.loadData();
    }

    //--//

    async loadData()
    {
        if (this.id)
        {
            let deploymentAgents = this.app.domain.deploymentAgents;

            deploymentAgents.logger.debug(`Loading Agent: ${this.id}`);
            let extended = await deploymentAgents.getExtendedById(this.id);
            if (!extended)
            {
                this.exit();
                return;
            }

            this.inject(DashboardManagementService)
                .recordAgent(extended);

            this.extended = extended;

            this.extendedRemoveChecks   = await this.extended.checkRemove();
            this.extendedNoRemoveReason = this.fromValidationToReason("Remove is disabled because:", this.extendedRemoveChecks);

            this.extendedTerminateChecks   = await this.extended.checkTerminate();
            this.extendedNoTerminateReason = this.fromValidationToReason("Terminate is disabled because:", this.extendedTerminateChecks);

            this.imageTag = "<unknown>";

            let task = await extended.findTask();
            if (task)
            {
                this.imageTag = task.model.image;

                let image = await task.getImage();
                if (image)
                {
                    let res = await image.selectTag();
                    if (res)
                    {
                        this.image    = res.taggedImage;
                        this.imageTag = res.desc;
                    }
                }
            }

            this.app.ui.navigation.breadcrumbCurrentLabel = extended.model.instanceId;
            deploymentAgents.logger.debug(`Loaded Agent: ${JSON.stringify(this.extended.model)}`);

            //--//

            this.removeAllDbSubscriptions();

            this.subscribeOneShot(extended,
                                  async (ext,
                                         action) =>
                                  {
                                      this.loadData();
                                  });

            if (this.shouldAutologin)
            {
                this.shouldAutologin        = false;
                this.tabGroup.selectedIndex = 1;
                this.loginShell();
            }
        }
    }

    save()
    {
        this.app.framework.errors.error("NOT_IMPLEMENTED", "This feature is not implemented.");
    }

    //--//

    get displayShutdownConfiguration(): boolean
    {
        return this.extended.model?.details?.shutdownConfiguration?.turnOffVoltage > 0;
    }

    get networkInterfaceKeys(): string[]
    {
        let details = this.extended.model.details;
        return UtilsService.extractKeysFromMap(details ? details.networkInterfaces : null, true);
    }

    getNetworkInterfaceValue(key: string): string
    {
        let details = this.extended.model.details;
        return details.networkInterfaces[key];
    }

    //--//

    async fetchThreads()
    {
        try
        {
            this.threadsFetching = true;
            this.threads         = await this.app.domain.apis.deploymentAgents.dumpThreads(this.id, true);
            if (!this.threads)
            {
                this.threads = "<unable to fetch threads...>";
            }
        }
        catch (e)
        {
            this.threads = "<failed to fetch threads>";
        }
        finally
        {
            this.threadsFetching = false;
        }
    }

    //--//

    @ReportError
    async loadLoggers()
    {
        try
        {
            this.loggersUpdating = true;
            this.detectChanges();

            this.loggers = await this.app.domain.apis.deploymentAgents.getLoggers(this.id);
        }
        finally
        {
            this.loggersUpdating = false;
        }

        this.detectChanges();
    }

    @ReportError
    async saveLoggers()
    {
        try
        {
            this.loggersUpdating = true;

            for (let logger of this.loggers)
            {
                if (this.loggersComp.wasUpdated(logger))
                {
                    await this.app.domain.apis.deploymentAgents.configLogger(this.id, logger);
                }
            }
        }
        finally
        {
            this.loggersUpdating = false;
        }

        this.resetLoggers();
    }

    resetLoggers()
    {
        this.loadLoggers();
    }

    //--//

    @ReportError
    async flushHeartbeat()
    {
        let result = await this.app.domain.apis.deploymentAgents.flush(this.id);
    }

    @ReportError
    async restart()
    {
        let result = await this.app.domain.apis.deploymentAgents.restart(this.id);
        if (result)
        {
            this.app.framework.errors.success(`Started agent restart...`, -1);
        }
        else
        {
            this.app.framework.errors.success(`Failed to restart agent...`, -1);
        }
    }

    //--//

    loginShell()
    {
        this.startShell("ssh:127.0.0.1");
    }

    newShell()
    {
        this.startShell(this.cmd);
    }

    @ReportError
    private async startShell(cmd: string)
    {

        this.log.refresh();
        this.log.interactive = true;

        this.shellOpening = true;
        try
        {
            this.app.domain.deploymentAgents.logger.debug(`Launching cmd '${cmd}' on Agent ${this.extended.model.instanceId}`);
            this.shellToken = await this.extended.openShell(cmd);
        }
        finally
        {
            this.shellOpening = false;
        }

        this.refreshLog();
    }

    @ReportError
    async closeShell()
    {
        this.log.interactive = false;

        if (this.shellToken)
        {
            await this.extended.closeShell(this.shellToken);
            this.shellToken = null;
        }

        this.shellState.flush();
    }

    cancelTimer()
    {
        if (this.shellTimer)
        {
            clearTimeout(this.shellTimer);
            this.shellTimer = null;
        }
    }

    async refreshLog()
    {
        this.cancelTimer();

        if (this.shellToken)
        {
            let res = await this.extended.readFromShell(this.shellToken);
            if (!res)
            {
                this.closeShell();
                return;
            }

            for (let line of res)
            {
                this.shellState.append(line.timestamp, line.fd, line.payload);
            }

            // Cancel again, after the await calls.
            this.cancelTimer();

            this.shellTimer = this.app.framework.utils.setTimeoutOutsideAngular(() => this.refreshLog(), 50);
        }
    }

    prepareDownload()
    {
        DownloadDialogComponent.openWithGenerator(this, "Shell Log", DownloadDialogComponent.fileName("shell", ".txt"), this.shellState);
    }

    onConsoleInputCommand(event: ConsoleLogCommandEventArgs)
    {
        if (this.shellToken)
        {
            this.extended.writeToShell(this.shellToken, event.command + "\n");
        }
    }

    @ReportError
    async terminate()
    {
        if (await this.confirmOperation("Click Yes to confirm termination of this agent."))
        {
            await this.extended.terminate();
        }
    }

    @ReportError
    async remove()
    {
        if (await this.confirmOperation("Click Yes to confirm deletion of this agent."))
        {
            this.removeAllDbSubscriptions();

            await this.extended.remove();

            this.exit();
        }
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    async goToImage()
    {
        if (this.image)
        {
            this.app.ui.navigation.go("/images/item", [
                this.image.model.sysId
            ]);
        }
    }
}

class ShellLogEntry implements IConsoleLogEntry
{
    public message: string;
    public columns = new Map<LogColumn, SafeHtml>();

    private timestamp: Date;

    constructor(private sanitizer: (html: string) => SafeHtml,
                public lineNumber: number,
                timestamp?: Date)
    {
        this.timestamp = timestamp || new Date();
    }

    updateHtml(message: string)
    {
        this.message = message;

        let renderer = new AnsiRenderer();

        let messageHtml   = renderer.toHtml(message);
        let timestampText = this.renderTimestamp();

        this.columns.set("lineNumber", this.sanitizer(`<span class="console-ln">${this.lineNumber}</span>`));
        this.columns.set("timestamp", this.sanitizer(`<span class="console-ts">${timestampText}</span>`));
        this.columns.set("line", this.sanitizer(`<span class="console-msg">${messageHtml}</span>`));
    }

    renderTimestamp(): string
    {
        return MomentHelper.parse(this.timestamp)
                           .format("YYYY-MM-DD HH:mm:ss.SSS");
    }
}

class ShellState implements IConsoleLogProvider,
                            DownloadGenerator
{
    private fd: number;
    private logEntry: ShellLogEntry;
    private buffer: string = "";

    private logEntries: ShellLogEntry[] = [];

    constructor(private sanitizer: (html: string) => SafeHtml,
                private log: ConsoleLogComponent)
    {
    }

    //--//

    getLogCount(): number
    {
        return this.logEntries.length;
    }

    async getLogPage(start: number,
                     end: number): Promise<IConsoleLogEntry[]>
    {
        return this.logEntries.slice(start, end);
    }

    public async performFilter(filter: ApplicationLogFilter): Promise<IApplicationLogRange[]>
    {
        return [];
    }

    //--//

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
        return true;
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
        return {lines: this.logEntries.map((logEntry) => `${logEntry.renderTimestamp()} ${logEntry.message}`)};
    }

    //--//

    flush()
    {
        this.logEntry = null;
        this.render(null, "## end of shell command ##");

        this.fd       = undefined;
        this.logEntry = null;
    }

    append(timestamp: Date,
           fd: number,
           payload: string)
    {
        if (this.fd != fd)
        {
            this.fd       = fd;
            this.logEntry = null;
        }

        this.buffer += payload;

        if (this.buffer.length > 0)
        {
            let endOfLine: number;

            while ((endOfLine = this.buffer.indexOf("\n")) >= 0)
            {
                this.render(timestamp, this.buffer.substring(0, endOfLine));

                this.buffer   = this.buffer.substring(endOfLine + 1);
                this.logEntry = null;
            }

            this.render(timestamp, this.buffer);
        }
    }

    render(timestamp: Date,
           text: string)
    {
        if (!this.logEntry)
        {
            this.logEntry = new ShellLogEntry(this.sanitizer, this.logEntries.length + 1, timestamp);
            this.logEntries.push(this.logEntry);
            this.log.refresh(false);
        }

        this.logEntry.updateHtml(text);
    }
}
