import {Component, EventEmitter, Injector, QueryList, ViewChild, ViewChildren} from "@angular/core";
import {NgForm} from "@angular/forms";
import {ReportError} from "app/app.service";
import {AlertDefinitionImportExport, AlertDefinitionImportHandler} from "app/customer/configuration/alert-rules/alert-rule-list-page.component";
import {AlertRuleBlocklyBlocks} from "app/customer/configuration/alert-rules/wizard/alert-rule-blockly-blocks";
import {AlertRuleInputFieldComponent} from "app/customer/configuration/alert-rules/wizard/alert-rule-input-field.component";
import {AlertRuleWizardDialogComponent, AlertRuleWizardState} from "app/customer/configuration/alert-rules/wizard/alert-rule-wizard-dialog.component";
import {ReportLivePreviewComponent} from "app/customer/configuration/reports/report-live-preview/report-live-preview.component";
import {AppBlocklyWorkspaceComponent} from "app/customer/engines/shared/workspace.component";
import {AlertExecutionReport} from "app/reports/definitions/alert-execution.report";
import {AlertDefinitionVersionExtended} from "app/services/domain/alert-definition-versions.service";
import {AlertDefinitionDetailsForUserProgramExtended, AlertDefinitionExtended} from "app/services/domain/alert-definitions.service";
import * as SharedSvc from "app/services/domain/base.service";
import {ReportConfig} from "app/services/domain/reporting.service";
import * as Models from "app/services/proxy/model/models";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";
import {convertLogFilters} from "app/shared/logging/application-log";
import {ApplicationLogFilter, IApplicationLogRange, IConsoleLogEntry, IConsoleLogProvider} from "framework/ui/consoles/console-log";
import {ConsoleLogComponent} from "framework/ui/consoles/console-log.component";
import {DownloadDialogComponent, DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";
import {ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {Future} from "framework/utils/concurrency";
import {debounceTime} from "rxjs/operators";

@Component({
               selector   : "o3-alert-rule-details-page",
               styleUrls  : ["./alert-rule-details-page.component.scss"],
               templateUrl: "./alert-rule-details-page.component.html"
           })
export class AlertRuleDetailsPageComponent extends SharedSvc.BaseComponentWithRouter implements IConsoleLogProvider
{
    alertDefinitionId: string;
    alertDefinition: AlertDefinitionExtended;
    alertVersions: AlertDefinitionVersionExtended[];
    head: AlertDefinitionVersionExtended;
    release: AlertDefinitionVersionExtended;
    selectedVersion: AlertDefinitionVersionExtended;
    currentDetails: AlertDefinitionDetailsForUserProgramExtended;

    logDialogConfig = OverlayConfig.newInstance({containerClasses: ["dialog-xl"]});

    blocks = AlertRuleBlocklyBlocks;

    blocklyDialogConfig = OverlayConfig.newInstance({
                                                        containerClasses: ["dialog-xl"],
                                                        showCloseButton : true
                                                    });

    testResultsDialogConfig = OverlayConfig.onTopDraggable({containerClasses: ["dialog-md"]});

    @ViewChild("log", {static: true}) log: ConsoleLogComponent;

    logLockScroll: boolean;
    private scrollToEnd: boolean = true;

    lastExecutionTime = new Date();
    maxSteps: number  = 10000;
    testResults: Models.AlertEngineExecutionStep[];
    testLogs: Models.LogLine[];
    reportConfig: ReportConfig;

    private editHead: AlertDefinitionVersionExtended;
    private editBase: AlertDefinitionVersionExtended;
    private minEditVersion: number;
    hasEditSession = false;
    needsSave      = false;

    testTimeRange: Models.RangeSelection = RangeSelectionExtended.newModel();

    changeListener = new EventEmitter<void>();

    @ViewChild("alertForm", {static: true}) alertForm: NgForm;
    @ViewChild("blocklyWorkspace") blocklyWorkspace: AppBlocklyWorkspaceComponent;
    @ViewChild("testResultsDialog", {static: true}) testResultsDialog: OverlayComponent;
    @ViewChild("reportLivePreview", {static: true}) reportPreviewDialog: ReportLivePreviewComponent;
    @ViewChildren(AlertRuleInputFieldComponent) inputFields: QueryList<AlertRuleInputFieldComponent>;

    public get inputs()
    {
        return this.currentDetails && this.currentDetails.data.getInputParameters() || [];
    }

    constructor(inj: Injector)
    {
        super(inj);

        this.changeListener
            .pipe(debounceTime(1000))
            .subscribe(() =>
                       {
                           this.save();
                       });
    }

    protected onNavigationComplete()
    {
        this.alertDefinitionId = this.getPathParameter("id");

        this.initAlertDefinition();
    }

    async initAlertDefinition()
    {
        this.alertDefinition = await this.app.domain.alertDefinitions.getExtendedById(this.alertDefinitionId);
        if (!this.alertDefinition)
        {
            this.exit();
            return;
        }

        this.app.ui.navigation.breadcrumbCurrentLabel = this.alertDefinition.model.title;

        let allVersions    = await this.alertDefinition.getAllVersions();
        this.alertVersions = allVersions.filter((v) => !v.getDetails().temporary);

        // Set edit base to first temp version if it exists
        [this.editBase] = allVersions.filter((v) => v.getDetails().temporary);

        this.release = await this.alertDefinition.getRelease();
        this.head    = await this.alertDefinition.getHead();

        if (!!this.editBase && this.head.getDetails().temporary)
        {
            this.hasEditSession = true;
        }

        this.selectedVersion = this.release;
        this.editHead        = this.selectedVersion;

        this.alertForm.form.markAsPristine();

        await this.initDetails();
    }

    async initDetails()
    {
        this.currentDetails = await this.editHead.getDetailsExtended();
        this.resetTestResults();
    }

    async versionChange(newId: string)
    {
        this.selectedVersion = await this.app.domain.alertDefinitionVersions.getExtendedById(newId);
        this.editHead        = this.selectedVersion;
        this.editBase        = null;
        await this.initDetails();
    }

    async importVersion()
    {
        let result = await ImportDialogComponent.open(this, "Import Alert Definition", new AlertDefinitionImportHandler(this.app.domain));
        if (result && result[0] && result[0].details)
        {
            // Only look at first version we see in the import
            this.currentDetails.model = result[0].details;
            this.needsSave            = true;
            await this.createNewVersion();

            // Update details so blockly refreshes
            await this.initDetails();
        }
    }

    async createNewVersion()
    {
        // Save temporary version with any changes
        await this.save();
        // Squash to new permanent version
        await this.saveEditSession();

        this.blocklyWorkspace.refreshBlocks();
    }

    async saveEditSession()
    {
        if (this.editHead && this.hasEditSession && this.editBase)
        {
            this.selectedVersion = await this.editHead.squash(this.editBase);

            this.alertVersions  = [
                this.selectedVersion,
                ...this.alertVersions
            ];
            this.head           = this.selectedVersion;
            this.minEditVersion = null;
            this.editBase       = null;
            this.hasEditSession = false;
            this.app.framework.errors.success("New version saved", -1);
        }
    }

    rebuildInputs()
    {
        this.inputFields.forEach((field) => field.rebuild());
    }

    @ReportError
    async saveTitle()
    {
        this.alertDefinition.save();
        this.exit();
    }

    @ReportError
    async save()
    {
        if (this.alertDefinition && !this.isPristine || this.needsSave)
        {
            this.needsSave = false;
            // Save a new version
            let newVersion = this.editHead.getNewVersion();
            if (!this.minEditVersion)
            {
                this.minEditVersion = newVersion.model.version;
                this.hasEditSession = true;
            }

            newVersion.model.details           = this.currentDetails.typedModel;
            newVersion.model.details.temporary = true;
            newVersion                         = await newVersion.save();
            this.editHead                      = newVersion;
            this.selectedVersion               = newVersion;
            this.head                          = newVersion;

            if (!this.editBase)
            {
                this.editBase = newVersion;
            }

            this.resetTestResults();
            this.blocklyWorkspace.markPristine();
            this.inputFields.forEach((input) => input.markPristine());
        }
    }

    @ReportError
    async test()
    {
        if (this.selectedVersion)
        {
            this.resetTestResults();
            try
            {
                if (this.lastExecutionTime)
                {
                    this.testResultsDialog.toggleOverlay();
                    let results      = await this.selectedVersion.evaluate(this.maxSteps, this.lastExecutionTime, true);
                    this.testResults = results.steps;
                    this.testLogs    = results.logEntries;
                }
            }
            catch (err)
            {
                this.testResultsDialog.toggleOverlay();
            }
        }
    }

    async runAlert()
    {
        if (this.selectedVersion)
        {
            let rangeExt = new RangeSelectionExtended(this.testTimeRange);
            let start    = rangeExt.getMin();
            let end      = rangeExt.getMax();

            let report = new AlertExecutionReport(this.app.domain);
            report.setAlert(this.alertDefinition.getIdentity(), this.selectedVersion.getIdentity());

            this.reportConfig = this.app.domain.reporting.getReportConfig(report.buildDynamicReport()
                                                                                .getDetails(),
                                                                          "On demand alert report",
                                                                          `Test of "${this.alertDefinition.model.title}"`,
                                                                          start.toDate(),
                                                                          end.toDate());

            this.reportPreviewDialog.toggleOverlay();
        }
    }

    public resetTestResults()
    {
        this.testResults = null;
        this.testLogs    = null;

        if (this.testResultsDialog.isOpen)
        {
            this.testResultsDialog.closeOverlay();
        }
    }

    public get isPristine(): boolean
    {
        return this.isVersionPristine && this.areInputFieldsPristine;
    }

    public get isVersionPristine(): boolean
    {
        return !this.blocklyWorkspace || !this.blocklyWorkspace.isDirty();
    }

    public get areInputFieldsPristine(): boolean
    {
        return this.inputFields && !this.inputFields.some((input) => !input.isPristine());
    }

    public get isInEditSession(): boolean
    {
        return this.editBase && this.editHead && this.editHead.model.version === this.head.model.version;
    }

    async edit()
    {
        let cfg = new AlertRuleWizardState(this.alertDefinitionId, this.selectedVersion.model.sysId, false, false);
        if (await AlertRuleWizardDialogComponent.open(cfg, this))
        {
            await this.initAlertDefinition();
            await this.versionChange(cfg.rules.version.model.sysId);
        }
    }

    async copy()
    {
        await AlertRuleWizardDialogComponent.open(new AlertRuleWizardState(this.alertDefinitionId, this.selectedVersion.model.sysId, false, true), this);
    }

    async exportDefinition()
    {
        let name      = `${this.alertDefinition.model.title}_v${this.selectedVersion.model.version}`;
        let timestamp = MomentHelper.fileNameFormat();

        DownloadDialogComponent.open<AlertDefinitionImportExport[]>(this, "Download Alert Rule", `alert-rule__${name}_${timestamp}.json`, [
            {
                definition: this.alertDefinition.getExport(),
                details   : this.selectedVersion.getDetails()
            }
        ]);
    }

    async makeRelease()
    {
        this.selectedVersion = await this.selectedVersion.makeRelease();
        this.release         = this.selectedVersion;
        await this.initDetails();
    }

    async undo()
    {
        this.editHead = await this.editHead.undo();
        await this.initDetails();
    }

    async redo()
    {
        this.editHead = await this.editHead.redo();
        await this.initDetails();
    }

    isRelease(id: string)
    {
        return this.release && id === this.release.model.sysId;
    }

    get isReleaseSelected(): boolean
    {
        return this.selectedVersion && this.isRelease(this.selectedVersion.model.sysId);
    }

    get canRedo(): boolean
    {
        return this.editHead && this.editHead.model.successors?.length > 0;
    }

    get canUndo(): boolean
    {
        return this.editHead && !!this.editHead.model.predecessor && this.minEditVersion && this.editHead.model.version >= this.minEditVersion;
    }

    async remove()
    {
        if (await this.confirmOperation("Click Yes to confirm deletion of this Alert Rule."))
        {
            await this.alertDefinition.remove();
            this.exit();
        }
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    //--//

    public getLogCount(): number
    {
        return this.alertDefinition ? this.alertDefinition.model.lastOffset : 0;
    }

    public async getLogPage(start: number,
                            end: number): Promise<IConsoleLogEntry[]>
    {
        let logEntries = [];

        let lines = await this.alertDefinition.getLog(start, end, null) || [];
        for (let line of lines)
        {
            logEntries.push(this.log.newLogEntry(line));
        }

        return logEntries;
    }

    public async performFilter(filters: ApplicationLogFilter): Promise<IApplicationLogRange[]>
    {
        return await this.alertDefinition.filterLog(convertLogFilters(filters)) || [];
    }

    async refreshLog()
    {
        this.subscribeOneShot(this.alertDefinition, async () =>
        {
            this.alertDefinition = await this.alertDefinition.refresh();
            this.refreshLog();
        });

        this.log.bind(this);

        this.logLockScroll = true;
        this.log.refresh(this.scrollToEnd);
        this.scrollToEnd = false;
    }

    prepareLogDownload()
    {
        if (this.selectedVersion)
        {
            let file = `${this.alertDefinition.model.title}`;

            DownloadDialogComponent.openWithGenerator(this, "Gateway Log", DownloadDialogComponent.fileName(file, ".txt"), new VersionLogDownloader(this.alertDefinition));
        }
    }

    async truncateLog(days?: number)
    {
        if (this.selectedVersion)
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

            await this.alertDefinition.deleteLog(minutes);

            this.log.reset();
        }
    }
}

class VersionLogDownloader implements DownloadGenerator
{
    logEntries: string[] = [];
    lastOffset: number   = 0;

    constructor(private extended: AlertDefinitionExtended)
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
            let log       = `${timestamp.format("YYYY-MM-DD HH:mm:ss")}: ${text}`;

            this.lastOffset = line.lineNumber + 1;

            this.logEntries.push(log);
        }

        return false;
    }

    public async sleepForProgress(): Promise<void>
    {
        await Future.delayed(500);
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

