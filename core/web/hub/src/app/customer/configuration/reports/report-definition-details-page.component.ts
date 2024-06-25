import {Component, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";
import {ReportError} from "app/app.service";
import {ReportDefinitionImportExport} from "app/customer/configuration/reports/report-list-page.component";
import {ReportWizardDialogComponent, ReportWizardState} from "app/customer/configuration/reports/wizard/report-wizard-dialog.component";
import * as SharedSvc from "app/services/domain/base.service";
import {ReportDefinitionVersionExtended} from "app/services/domain/report-definition-versions.service";
import {ReportDefinitionExtended} from "app/services/domain/report-definitions.service";
import {SchedulingType} from "app/services/domain/reporting.service";
import {DaysOfWeek} from "app/shared/forms/time-range/range-selection-extended";
import {UtilsService} from "framework/services/utils.service";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import moment from "framework/utils/moment";
import {ReportHistoryListComponent} from "./report-history-list.component";
import {ReportViewDialogComponent} from "./report-view-dialog.component";

@Component({
               selector   : "o3-report-details-page",
               styleUrls  : ["./report-definition-details-page.component.scss"],
               templateUrl: "./report-definition-details-page.component.html"
           })
export class ReportDefinitionDetailsPageComponent extends SharedSvc.BaseComponentWithRouter
{
    reportDefinitionID: string;
    reportDefinition: ReportDefinitionExtended;
    reportVersions: ReportDefinitionVersionExtended[];
    head: ReportDefinitionVersionExtended;
    release: ReportDefinitionVersionExtended;
    selectedVersion: ReportDefinitionVersionExtended;
    reportScheduleText: string = null;
    reportDeliveryText: string = null;

    @ViewChild("reportForm", {static: true}) reportForm: NgForm;

    @ViewChild("historyList", {static: true}) historyList: ReportHistoryListComponent;

    protected onNavigationComplete()
    {
        this.reportDefinitionID = this.getPathParameter("id");

        this.initReport();
    }

    async initReport()
    {
        this.reportDefinition = await this.app.domain.reportDefinitions.getExtendedById(this.reportDefinitionID);
        if (!this.reportDefinition)
        {
            this.exit();
            return;
        }

        this.app.ui.navigation.breadcrumbCurrentLabel = this.reportDefinition.model.title;

        this.reportVersions = await this.reportDefinition.getAllVersions();
        this.head           = await this.reportDefinition.getHead();
        this.release        = await this.reportDefinition.getRelease();

        if (!this.selectedVersion)
        {
            this.selectedVersion = this.head;
        }

        this.initVersion();
    }

    initVersion()
    {
        let schedule = this.selectedVersion?.getDetailsExtended()
                           .getSchedulingOptions();

        this.reportScheduleText = null;
        this.reportDeliveryText = null;
        if (schedule && schedule.model.schedule)
        {
            let time                = MomentHelper.parse(schedule.timeOfDay);
            let zone                = MomentHelper.timeZone(schedule.model.schedule.zoneDesired)
                                                  .abbr(new Date().valueOf());
            this.reportScheduleText = `${schedule.schedulingType} at ${time.format("LT")} ${zone}`;
            switch (schedule.schedulingType)
            {
                case SchedulingType.Daily:
                    if (schedule.daysOfWeek.length !== 7)
                    {
                        let days = DaysOfWeek.filter((d) => schedule.daysOfWeek.find((day) => day === d))
                                             .map((d) => d.substr(0, 3))
                                             .join(", ");
                        this.reportScheduleText += ` on ${UtilsService.capitalizeFirstLetterAllWords(days)}`;
                    }
                    break;

                case SchedulingType.Weekly:
                    this.reportScheduleText += ` on ${UtilsService.capitalizeFirstLetterAllWords(schedule.daysOfWeek[0])}`;
                    break;

                case SchedulingType.Monthly:
                    this.reportScheduleText += ` on day ${schedule.dayOfMonth} of the month`;
                    break;

                case SchedulingType.OnDemand:
                    this.reportScheduleText = `Not Scheduled`;
                    break;
            }
        }
        else
        {
            this.reportScheduleText = "Not Scheduled";
        }

        this.reportDeliveryText = schedule?.deliveryOptions?.getDisplayText(true) ?? "No users or roles selected";
    }

    async versionChange(newId: string)
    {
        this.selectedVersion = await this.app.domain.reportDefinitionVersions.getExtendedById(newId);
        this.initVersion();
    }

    @ReportError
    async save()
    {
        if (this.reportDefinition)
        {
            await this.reportDefinition.save();

            this.reportForm.form.markAsPristine();

            this.app.framework.errors.success("Report updated", -1);
        }
    }

    async configure(step: string)
    {
        if (await ReportWizardDialogComponent.open(new ReportWizardState(this.reportDefinitionID, this.selectedVersion.model.sysId, step), this))
        {
            this.selectedVersion = null;
            await this.refresh();
        }
    }

    async edit()
    {
        await this.configure("name");
    }

    async exportDefinition()
    {
        let timestamp = MomentHelper.fileNameFormat();
        let title     = this.reportDefinition.model.title;
        DownloadDialogComponent.open<ReportDefinitionImportExport>(this, "Download Report Configuration", `report__${title}__${timestamp}.json`, {
            reportDefinitions       : [this.reportDefinition.model],
            reportDefinitionVersions: [this.selectedVersion.model]
        });
    }

    async makeRelease()
    {
        await this.selectedVersion.makeRelease();
        await this.refresh();
    }

    async undo()
    {
        this.selectedVersion = await this.selectedVersion.undo();
        await this.refresh();
    }

    async redo()
    {
        this.selectedVersion = await this.selectedVersion.redo();
        await this.refresh();
    }

    isRelease(id: string)
    {
        return this.release && id === this.release.model.sysId;
    }

    isHead(id: string)
    {
        return this.head && id === this.head.model.sysId;
    }

    get isReleaseSelected(): boolean
    {
        return this.selectedVersion && this.isRelease(this.selectedVersion.model.sysId);
    }

    get isHeadSelected(): boolean
    {
        return this.selectedVersion && this.isHead(this.selectedVersion.model.sysId);
    }

    get hasPredecessor(): boolean
    {
        return this.selectedVersion && !!this.selectedVersion.model.predecessor;
    }

    private async refresh()
    {
        await this.reportDefinition.flush();
        this.reportDefinition = <any>await this.reportDefinition.refresh();
        await this.initReport();
    }

    async openViewDialog()
    {
        let detailsExt = this.selectedVersion.getDetailsExtended();
        let result     = await ReportViewDialogComponent.open(this, detailsExt.getSchedulingOptions(), undefined, "400px");

        if (result && result.range)
        {
            this.view(result.range.minAsMoment, result.range.maxAsMoment);
        }
    }

    private async view(startDate: moment.Moment,
                       endDate: moment.Moment)
    {
        // generate a new report instance
        await this.selectedVersion.triggerReport(startDate.toDate(), endDate.toDate());

        setTimeout(() => this.initReport(), 1000);
    }

    async remove()
    {
        if (await this.confirmOperation("This report cannot be recovered"))
        {
            await this.reportDefinition.remove();
            this.exit();
        }
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
