import {Component, Input} from "@angular/core";
import {ReportPreviewDialogComponent, ReportPreviewDialogConfig} from "app/customer/configuration/reports/wizard/report-preview-dialog.component";
import {ReportWizardState} from "app/customer/configuration/reports/wizard/report-wizard-dialog.component";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {ReportDefinitionVersionExtended} from "app/services/domain/report-definition-versions.service";
import {ReportDefinitionExtended} from "app/services/domain/report-definitions.service";
import {ReportExtended} from "app/services/domain/reports.service";
import * as Models from "app/services/proxy/model/models";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";
import moment from "framework/utils/moment";

@Component({
               selector: "o3-report-preview-button",
               template: `
                   <button mat-raised-button color="primary" type="button" (click)="openViewDialog()" [disabled]="loading || !isValid">
                       <span *ngIf="!loading">Preview Report</span>
                       <span *ngIf="loading">Generating <i class="fa fa-spin fa-spinner"></i></span>
                   </button>
               `
           })
export class ReportPreviewButtonComponent extends BaseApplicationComponent
{
    loading: boolean = false;

    private previewDialogData: ReportPreviewDialogConfig;

    previewReport: ReportExtended;
    previewReportDefinition: ReportDefinitionExtended;
    previewVersion: ReportDefinitionVersionExtended;
    previewHead: ReportDefinitionVersionExtended;

    @Input() data: ReportWizardState;

    get isValid(): boolean
    {
        return !!this.data.reportDefinition.model.title;
    }

    async savePreview(): Promise<boolean>
    {
        // Create a new report definition that will be marked for auto-delete. To display the preview,
        // we don't need to save the current report definition.
        this.previewReportDefinition                   = this.app.domain.reportDefinitions.allocateInstance();
        this.previewVersion                            = this.app.domain.reportDefinitionVersions.allocateInstance();
        // Fill in model properties.
        let tomorrowDate                               = moment(new Date())
            .add(1, "days")
            .toDate();
        this.previewReportDefinition.model.autoDelete  = tomorrowDate;
        this.previewReportDefinition.model.active      = false;
        this.previewReportDefinition.model.description = this.data.reportDefinition.model.description;
        this.previewReportDefinition.model.title       = this.data.reportDefinition.model.title;
        this.previewReportDefinition.model.user        = this.data.reportDefinition.model.user;
        // Associate report details.
        this.data.report.synchronize();
        this.previewVersion.model.details = this.data.version.model.details;

        // Save definition and version.
        this.previewReportDefinition         = await this.previewReportDefinition.save();
        this.previewVersion.model.definition = this.previewReportDefinition.getIdentity();
        this.previewHead                     = await this.previewVersion.save();

        return true;
    }

    async openViewDialog()
    {
        if (!this.previewHead)
        {
            await this.savePreview();
        }

        if (this.previewHead)
        {
            let headDetailsExt = this.previewHead.getDetailsExtended();
            let reportRangeExt = RangeSelectionExtended.fromTimeRangeId(headDetailsExt.getSchedulingOptions().model.range);
            let chartRange     = reportRangeExt.getChartRange();

            if (chartRange) this.viewPreview(chartRange.minAsMoment, chartRange.maxAsMoment);
        }
    }

    private async viewPreview(startDate: moment.Moment,
                              endDate: moment.Moment)
    {
        // generate a new report instance
        this.previewReport = await this.previewHead.triggerReport(startDate.toDate(), endDate.toDate());

        this.openPreviewDialog();

        // Wait for our report's status to be updated, to retrieve the download URL.
        while (true)
        {
            let updatedReport = await this.waitForChange<ReportExtended, Models.Report>(this.previewReport);
            if (!updatedReport) break;

            if (updatedReport.isFinished)
            {
                this.previewReport = updatedReport;

                if (this.previewDialogData)
                {
                    this.previewDialogData.reportUrl = this.previewReport.getDownloadUrl(this.previewReportDefinition.model.title);
                }
                break;
            }

            if (updatedReport.isFailed)
            {
                if (this.previewDialogData)
                {
                    this.previewDialogData.failed = true;
                }
                break;
            }
        }

        this.previewHead = null;
        this.loading     = false;
    }

    private openPreviewDialog()
    {
        if (!this.previewReport) return;

        this.previewDialogData = {};

        ReportPreviewDialogComponent.open(this, this.previewDialogData, true);
    }
}
