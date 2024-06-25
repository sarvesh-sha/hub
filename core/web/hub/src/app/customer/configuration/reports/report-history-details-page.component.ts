import {Component, Injector, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";
import * as SharedSvc from "app/services/domain/base.service";
import {ReportDefinitionExtended} from "app/services/domain/report-definitions.service";
import {ReportExtended} from "app/services/domain/reports.service";

@Component({
               selector   : "o3-report-history-details-page",
               templateUrl: "./report-history-details-page.component.html"
           })
export class ReportHistoryDetailsPageComponent extends SharedSvc.BaseComponentWithRouter
{
    reportDefinitionID: string;
    reportID: string;
    reportDefinition: ReportDefinitionExtended;
    report: ReportExtended;
    reportUrl: string;

    @ViewChild("reportForm", {static: true}) reportForm: NgForm;

    get isFinished()
    {
        if (this.report) return this.report.isFinished;
        return false;
    }

    constructor(inj: Injector)
    {
        super(inj);
    }

    protected onNavigationComplete()
    {
        this.reportDefinitionID = this.getPathParameter("id");
        this.reportID           = this.getPathParameter("historyID");
        this.initReport();
    }

    async initReport()
    {
        this.reportDefinition                         = await this.app.domain.reportDefinitions.getExtendedById(this.reportDefinitionID);
        this.report                                   = await this.app.domain.reports.getExtendedById(this.reportID);
        this.app.ui.navigation.breadcrumbCurrentLabel = "History Details";
        this.reportUrl                                = this.report.getDownloadUrl(this.reportDefinition.model.title);
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    view()
    {
        window.open(this.reportUrl, "_blank");
    }

    debug()
    {
        let url = "/#/reports;sys_asreport=true;token=" + this.report.model.sysId;
        window.open(url, "_blank");
    }

    remove()
    {
        this.app.framework.errors.error("NOT_IMPLEMENTED", "Deleting the report is not implemented.");
    }
}
