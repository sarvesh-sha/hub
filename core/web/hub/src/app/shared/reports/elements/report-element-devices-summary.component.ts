import {Component, Injector} from "@angular/core";
import {DeviceSummaryValues} from "app/services/domain/widget-data.service";
import {ReportElementBaseComponent, ReportElementConfigurationBase, ReportElementDataBase, ReportElementModel, ReportElementType} from "app/shared/reports/elements/report-element-base.component";

@Component({
               selector   : "o3-report-element-devices-summary",
               templateUrl: "./report-element-devices-summary.component.html"
           })
export class ReportElementDevicesSummaryComponent extends ReportElementBaseComponent<ReportElementDevicesSummaryData, ReportElementDevicesSummaryConfiguration>
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

export class ReportElementDevicesSummaryData extends ReportElementDataBase
{
    aggregates: DeviceSummaryValues;
}

export class ReportElementDevicesSummaryConfiguration extends ReportElementConfigurationBase
{
    public static newReportModel()
    {
        let model           = new ReportElementModel();
        model.type          = ReportElementType.DevicesSummary;
        model.configuration = new ReportElementDevicesSummaryConfiguration();
        return model;
    }
}
