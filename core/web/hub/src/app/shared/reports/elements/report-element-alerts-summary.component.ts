import {Component, Injector} from "@angular/core";
import {AlertSummaryValues} from "app/services/domain/widget-data.service";
import {ReportElementBaseComponent, ReportElementConfigurationBase, ReportElementDataBase, ReportElementModel, ReportElementType} from "app/shared/reports/elements/report-element-base.component";

@Component({
               selector   : "o3-report-element-alerts-summary",
               templateUrl: "./report-element-alerts-summary.component.html"
           })
export class ReportElementAlertsSummaryComponent extends ReportElementBaseComponent<ReportElementAlertsSummaryData, ReportElementAlertsSummaryConfiguration>
{
    constructor(inj: Injector)
    {
        super(inj);
    }

    afterConfigurationChanges()
    {
        this.markAsComplete();
    }
}

export class ReportElementAlertsSummaryData extends ReportElementDataBase
{
    aggregates: AlertSummaryValues;
}

export class ReportElementAlertsSummaryConfiguration extends ReportElementConfigurationBase
{
    public static newReportModel()
    {
        let model           = new ReportElementModel();
        model.type          = ReportElementType.AlertsSummary;
        model.configuration = new ReportElementAlertsSummaryConfiguration();
        return model;
    }
}
