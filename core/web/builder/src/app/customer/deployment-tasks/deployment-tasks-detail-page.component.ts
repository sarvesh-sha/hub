import {Component, Injector, ViewChild} from "@angular/core";

import {ReportError} from "app/app.service";

import * as SharedSvc from "app/services/domain/base.service";
import {DeploymentTaskExtended} from "app/services/domain/deployment-tasks.service";
import {RegistryTaggedImageExtended} from "app/services/domain/registry-tagged-images.service";
import * as Models from "app/services/proxy/model/models";

import {convertLogFilters} from "app/shared/logging/application-log";

import {ApplicationLogFilter, IApplicationLogRange, IConsoleLogEntry, IConsoleLogProvider} from "framework/ui/consoles/console-log";
import {ConsoleLogComponent} from "framework/ui/consoles/console-log.component";
import {DownloadDialogComponent, DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

@Component({
               selector   : "o3-deployment-tasks-detail-page",
               templateUrl: "./deployment-tasks-detail-page.component.html"
           })
export class DeploymentTasksDetailPageComponent extends SharedSvc.BaseComponentWithRouter implements IConsoleLogProvider
{
    id: string;
    extended: DeploymentTaskExtended;
    extendedRemoveChecks: Models.ValidationResult[];
    extendedNoRemoveReason: string;

    extendedTerminateChecks: Models.ValidationResult[];
    extendedNoTerminateReason: string;

    extendedRestartChecks: Models.ValidationResult[];
    extendedNoRestartReason: string;

    imageTag: string;
    image: RegistryTaggedImageExtended;

    modelMountKeys: string[]  = [];
    modelLabelsKeys: string[] = [];
    modelLabels: { [key: string]: LabelDetails; };

    @ViewChild("log", {static: true}) log: ConsoleLogComponent;

    logLockScroll: boolean;
    scrollToEnd = true;
    private logRefreshTimeout: any;
    private logPreviousTask: DeploymentTaskExtended;

    constructor(inj: Injector)
    {
        super(inj);

        this.extended = this.app.domain.deploymentTasks.allocateInstance();
    }

    ngOnDestroy()
    {
        super.ngOnDestroy();

        this.extended = null;
        this.cancelRefresh();
    }

    protected async onNavigationComplete()
    {
        this.id = this.getPathParameter("id");

        this.loadData();
    }

    //--//

    private async loadData()
    {
        if (this.id)
        {
            let deploymentTasks = this.app.domain.deploymentTasks;

            this.log.bind(this);

            deploymentTasks.logger.debug(`Loading Task: ${this.id}`);
            let extended = await deploymentTasks.getExtendedById(this.id);
            if (!extended)
            {
                this.exit();
                return;
            }

            this.extended = extended;

            this.extendedRemoveChecks   = await this.extended.checkRemove();
            this.extendedNoRemoveReason = this.fromValidationToReason("Remove is disabled because:", this.extendedRemoveChecks);

            this.extendedTerminateChecks   = await this.extended.checkTerminate();
            this.extendedNoTerminateReason = this.fromValidationToReason("Terminate is disabled because:", this.extendedTerminateChecks);

            this.extendedRestartChecks   = await this.extended.checkRestart();
            this.extendedNoRestartReason = this.fromValidationToReason("Terminate is disabled because:", this.extendedRestartChecks);

            this.imageTag = extended.model.image;

            let image = await extended.getImage();
            if (image)
            {
                let res = await image.selectTag();
                if (res)
                {
                    this.image    = res.taggedImage;
                    this.imageTag = res.desc;
                }
            }

            let keys                                    = [];
            let labels: { [key: string]: LabelDetails } = {};
            for (let key in this.extended.model.labels)
            {
                let details = new LabelDetails();

                keys.push(key);

                let value = this.extended.model.labels[key];
                if (key == "Optio3_ConfigTemplate")
                {
                    value = window.atob(value);
                }

                details.labelLong = value;
                if (value.length > 40)
                {
                    details.labelShort = value.substr(0, 40) + " ...";
                }
                else
                {
                    details.labelShort = value;
                }

                labels[key] = details;
            }
            this.modelLabelsKeys = keys;
            this.modelLabels     = labels;

            let modelMountKeys = [];
            for (let key in this.extended.model.mounts)
            {
                modelMountKeys.push(key);
            }
            this.modelMountKeys = modelMountKeys;

            if (!this.logPreviousTask || this.logPreviousTask.model.sysId != this.id)
            {
                this.log.reset();
                this.logPreviousTask = extended;
            }

            this.app.ui.navigation.breadcrumbCurrentLabel = extended.model.dockerId;
            deploymentTasks.logger.debug(`Loaded Task: ${JSON.stringify(this.extended.model)}`);

            //--//

            this.removeAllDbSubscriptions();

            this.subscribeOneShot(extended,
                                  async (ext,
                                         action) =>
                                  {
                                      this.loadData();
                                  });

            this.logLockScroll = true;
            this.log.refresh(this.scrollToEnd);
            this.scrollToEnd = false;
        }
    }

    //--//

    async pullLog()
    {
        await this.waitUntilTrue(10, () => !!this.extended);

        this.cancelRefresh();
        this.log.refresh(false);

        if (this.extended && this.extended.model)
        {
            this.extended.getLog(this.extended.model.lastOffset, 0, 1000);

            this.logRefreshTimeout = this.app.framework.utils.setTimeoutOutsideAngular(() => this.pullLog(), 500);
        }
    }

    private cancelRefresh()
    {
        if (this.logRefreshTimeout)
        {
            clearTimeout(this.logRefreshTimeout);
            this.logRefreshTimeout = undefined;
        }
    }

    //--//

    @ReportError
    async restart()
    {
        if (await this.confirmOperation("Click Yes to confirm restart of this task."))
        {
            await this.extended.restart();

            this.exit();
        }
    }

    @ReportError
    async terminate()
    {
        if (await this.confirmOperation("Click Yes to confirm termination of this task."))
        {
            await this.extended.terminate();

            this.exit();
        }
    }

    @ReportError
    async remove()
    {
        if (await this.confirmOperation("Click Yes to confirm deletion of this task."))
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

    //--//

    getLogCount(): number
    {
        return this.extended ? this.extended.model.lastOffset : 0;
    }

    async getLogPage(start: number,
                     end: number): Promise<IConsoleLogEntry[]>
    {
        let logEntries = [];

        let lines = await this.extended.getLog(start, end, null) || [];
        for (let line of lines)
        {
            logEntries.push(this.log.newLogEntry(line));
        }

        return logEntries;
    }

    async performFilter(filters: ApplicationLogFilter): Promise<IApplicationLogRange[]>
    {
        return await this.extended.filterLog(convertLogFilters(filters)) || [];
    }

    //--//

    prepareDownload()
    {
        if (this.extended && this.extended.model)
        {
            DownloadDialogComponent.openWithGenerator(this, "Task Log", DownloadDialogComponent.fileName(this.extended.model.name, ".txt"), new TaskLogDownloader(this.extended));
        }
    }
}

class TaskLogDownloader implements DownloadGenerator
{
    logEntries: string[] = [];
    lastOffset: number   = 0;

    constructor(private extended: DeploymentTaskExtended)
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

        for (let line of lines)
        {
            let text      = line.line.replace("\n", "");
            let timestamp = MomentHelper.parse(line.timestamp);
            let log       = `${timestamp.format("YYYY-MM-DD HH:mm:ss.SSS")}: ${text}\n`;

            this.lastOffset = line.lineNumber + 1;

            this.logEntries.push(log);
        }

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
        return {lines: this.logEntries};
    }
}

class LabelDetails
{
    labelShort: string;
    labelLong: string;
}
