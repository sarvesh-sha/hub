import {Component, Injector, ViewChild} from "@angular/core";
import {ReportError} from "app/app.service";

import {JobImagesListComponent} from "app/customer/job-images/job-images-list.component";
import {JobStepsListComponent} from "app/customer/job-steps/job-steps-list.component";

import * as SharedSvc from "app/services/domain/base.service";
import {JobStepExtended} from "app/services/domain/job-steps.service";
import {JobExtended} from "app/services/domain/jobs.service";
import * as Models from "app/services/proxy/model/models";

import {convertLogFilters} from "app/shared/logging/application-log";

import {ApplicationLogFilter, IApplicationLogRange, IConsoleLogEntry, IConsoleLogProvider} from "framework/ui/consoles/console-log";
import {ConsoleLogComponent} from "framework/ui/consoles/console-log.component";
import {DownloadDialogComponent, DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {TabGroupComponent} from "framework/ui/tab-group/tab-group.component";

@Component({
               selector   : "o3-jobs-detail-page",
               templateUrl: "./jobs-detail-page.component.html"
           })
export class JobsDetailPageComponent extends SharedSvc.BaseComponentWithRouter implements IConsoleLogProvider
{
    id: string;
    extended: JobExtended;
    extendedRemoveChecks: Models.ValidationResult[];
    extendedNoRemoveReason: string;

    ranges: LogRange[]          = [];
    jobSources: JobSourceInfo[] = [];

    canMarkAsReleaseCandidate = false;
    isReleaseCandidate        = false;

    canMarkAsRelease = false;
    isRelease        = false;

    @ViewChild("tabGroup", {static: true}) tabGroup: TabGroupComponent;

    @ViewChild("childListSteps") childListSteps: JobStepsListComponent;

    @ViewChild("childListImages") childListImages: JobImagesListComponent;

    @ViewChild("log", {static: true}) log: ConsoleLogComponent;

    lockScroll: boolean;
    scrollToEnd: boolean;

    constructor(inj: Injector)
    {
        super(inj);

        this.scrollToEnd = true;
        this.extended    = this.app.domain.jobs.allocateInstance();
    }

    protected onNavigationComplete()
    {
        this.id = this.getPathParameter("id");

        this.refresh();
    }

    async refresh()
    {
        if (this.id)
        {
            let jobs = this.app.domain.jobs;

            this.log.bind(this);

            jobs.logger.debug(`Loading Job: ${this.id}`);
            let extended = await jobs.getExtendedById(this.id);
            if (!extended)
            {
                this.exit();
                return;
            }

            this.lockScroll             = extended.model.status == Models.JobStatus.EXECUTING;
            this.extended               = extended;
            this.extendedRemoveChecks   = await this.extended.checkRemove();
            this.extendedNoRemoveReason = this.fromValidationToReason("Remove is disabled because:", this.extendedRemoveChecks);

            this.canMarkAsReleaseCandidate = false;
            this.isReleaseCandidate        = false;

            this.canMarkAsRelease = false;
            this.isRelease        = false;

            if (extended.model.status == Models.JobStatus.COMPLETED)
            {
                for (let image of await extended.getImages())
                {
                    switch (image.model.releaseStatus)
                    {
                        case Models.RegistryImageReleaseStatus.None:
                            this.canMarkAsReleaseCandidate = true;
                            break;

                        case Models.RegistryImageReleaseStatus.ReleaseCandidate:
                            this.isReleaseCandidate = true;
                            this.canMarkAsRelease   = true;
                            break;

                        case Models.RegistryImageReleaseStatus.Release:
                            this.isRelease        = true;
                            this.canMarkAsRelease = true;
                            break;
                    }
                }
            }

            let ranges = [];
            let steps  = await extended.getSteps();

            let offset = 0;
            for (let step of steps)
            {
                let rangeHeader = new LogRange();

                rangeHeader.localState = [
                    this.newLine(0, step.model.createdOn, ""),
                    this.newLine(0, step.model.createdOn, "#############################################"),
                    this.newLine(0, step.model.createdOn, `Log for step ${step.model.name}`),
                    this.newLine(0, step.model.createdOn, "#############################################"),
                    this.newLine(0, step.model.createdOn, "")
                ];

                rangeHeader.start = offset;
                rangeHeader.end   = (offset += rangeHeader.localState.length);
                ranges.push(rangeHeader);

                let rangeBody         = new LogRange();
                rangeBody.start       = offset;
                rangeBody.end         = (offset += step.model.lastOffset);
                rangeBody.remoteState = step;
                ranges.push(rangeBody);
            }

            this.ranges = ranges;

            //--//

            let sources = [];

            for (let source of await this.extended.getSources())
            {
                let item = new JobSourceInfo();

                let repo = await source.getRepo();
                if (repo)
                {
                    item.repo = repo.model.gitUrl;
                }

                item.branch = source.model.branch;
                item.commit = source.model.commit;

                item.commitShort = item.commit.substr(0, 7);

                let commit = await source.getCommit();
                if (commit)
                {
                    item.comment = commit.model.message;
                }

                sources.push(item);
            }

            this.jobSources = sources;

            //--//

            this.app.ui.navigation.breadcrumbCurrentLabel = extended.model.name;
            jobs.logger.debug(`Loaded Job: ${JSON.stringify(this.extended.model)}`);

            //--//

            this.removeAllDbSubscriptions();

            this.subscribeOneShot(extended,
                                  async (ext,
                                         action) =>
                                  {
                                      this.refresh();
                                  });

            for (let step of steps)
            {
                this.subscribeOneShot(step,
                                      async (ext,
                                             action) =>
                                      {
                                          this.refresh();
                                      });
            }

            this.log.refresh(this.scrollToEnd && this.lockScroll);
            this.scrollToEnd = false;
        }
    }

    //--//

    getLogCount(): number
    {
        let count = 0;

        for (let range of this.ranges)
        {
            count += (range.end - range.start);
        }

        return count;
    }

    async getLogPage(start: number,
                     end: number): Promise<IConsoleLogEntry[]>
    {
        let res = [];

        for (let range of this.ranges)
        {
            if (range.start <= start && start < range.end)
            {
                if (range.localState)
                {
                    while (start < end && start < range.end)
                    {
                        res.push(this.log.newLogEntry(range.localState[start - range.start], false));
                        start++;
                    }
                }
                else
                {
                    let endInRange = Math.min(range.end, end);

                    let lines = await range.remoteState.getLog(start - range.start, endInRange - range.start, null);
                    for (let line of lines)
                    {
                        res.push(this.log.newLogEntry(line));
                    }

                    start = endInRange;
                }
            }
        }

        return res;
    }

    async performFilter(filters: ApplicationLogFilter): Promise<IApplicationLogRange[]>
    {
        let ranges: Models.LogRange[] = [];
        let convertedFilters          = convertLogFilters(filters);
        for (let range of this.ranges)
        {
            if (range.localState)
            {
                ranges.push(Models.LogRange.newInstance({
                                                            startOffset: range.start,
                                                            endOffset  : range.end - 1
                                                        }));
            }
            else
            {
                let stepRanges = await range.remoteState.filterLog(convertedFilters);
                for (let stepRange of stepRanges)
                {
                    ranges.push(Models.LogRange.newInstance({
                                                                startOffset: range.start + stepRange.startOffset,
                                                                endOffset  : range.start + stepRange.endOffset
                                                            }));
                }
            }
        }

        return ranges;
    }

    newLine(line: number,
            timestamp: Date,
            message: string): Models.LogLine
    {
        return Models.LogLine.newInstance({
                                              lineNumber: line,
                                              timestamp : timestamp,
                                              line      : message
                                          });
    }

    @ReportError
    async remove()
    {
        if (await this.confirmOperation("Click Yes to confirm deletion of this job."))
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

    @ReportError
    async markAsReleaseCandidate()
    {
        for (let image of await this.extended.getImages())
        {
            await image.markForRelease(Models.RegistryImageReleaseStatus.ReleaseCandidate);

            this.app.framework.errors.success(`Marked image ${image.model.tag} as RC`, -1);
        }
    }

    @ReportError
    async markAsRelease()
    {
        for (let image of await this.extended.getImages())
        {
            await image.markForRelease(Models.RegistryImageReleaseStatus.Release);

            this.app.framework.errors.success(`Marked image ${image.model.tag} as RTM`, -1);
        }
    }

    @ReportError
    async distribute()
    {
        let count = 0;

        for (let image of await this.extended.getImages())
        {
            switch (image.model.releaseStatus)
            {
                case Models.RegistryImageReleaseStatus.ReleaseCandidate:
                case Models.RegistryImageReleaseStatus.Release:
                    count += await image.distribute(Models.DeploymentOperationalStatus.operational);
            }
        }

        this.app.framework.errors.success(`Starting ${count} downloads...`, -1);
    }

    async stopJob()
    {
        await this.extended.cancel();
    }

    downloadLog()
    {
        if (this.extended && this.extended.model)
        {
            DownloadDialogComponent.openWithGenerator(this, "Job Log", DownloadDialogComponent.fileName(this.extended.model.name, ".txt"), new LogDownloader(this.extended));
        }
    }

    jobNotExecuting()
    {
        return this.extended.model.status != Models.JobStatus.EXECUTING;
    }
}

class JobSourceInfo
{
    repo: string;
    branch: string;
    commit: string;
    commitShort: string;
    comment: string;
}

class LogRange
{
    start: number;
    end: number;

    localState: Models.LogLine[];
    remoteState: JobStepExtended;
}

class LogDownloader implements DownloadGenerator
{
    private logEntries: string[] = [];

    private steps: JobStepExtended[];
    private activeStep: JobStepExtended;
    private activeStepIndex: number  = 0;
    private activeStepOffset: number = 0;

    constructor(private extended: JobExtended)
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
        if (!this.steps)
        {
            this.steps = await this.extended.getSteps();
        }

        if (this.activeStepIndex >= this.steps.length)
        {
            return true;
        }

        if (!this.activeStep)
        {
            let step = this.steps[this.activeStepIndex];

            this.pushLine(0, step.model.createdOn, "");
            this.pushLine(0, step.model.createdOn, "#############################################");
            this.pushLine(0, step.model.createdOn, `Log for step ${step.model.name}`);
            this.pushLine(0, step.model.createdOn, "#############################################");
            this.pushLine(0, step.model.createdOn, "");

            this.activeStep = step;
        }

        let lastOffset = this.activeStep.model.lastOffset;
        if (this.activeStepOffset >= lastOffset)
        {
            this.activeStep       = null;
            this.activeStepOffset = 0;
            this.activeStepIndex++;
        }
        else
        {
            let lines = await this.activeStep.getLog(this.activeStepOffset, lastOffset, 4357); // Random-looking number, better for the UI, less flickering

            for (let line of lines)
            {
                this.pushLine(line.lineNumber + 1, line.timestamp, line.line);

                this.activeStepOffset = line.lineNumber + 1;
            }
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

    private pushLine(line: number,
                     timestamp: Date,
                     message: string)
    {
        message = message.replace("\n", "");

        let timestampParsed = MomentHelper.parse(timestamp);
        let log             = `${timestampParsed.format("YYYY-MM-DD HH:mm:ss.SSS")}: ${line} ${message}`;

        this.logEntries.push(log);
    }
}
