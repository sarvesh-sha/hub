import {Component} from "@angular/core";

import * as Models from "app/services/proxy/model/models";
import {AlertTableConfig} from "app/shared/alerts/alert-table/alert-table.component";
import {ReportElementBaseComponent, ReportElementConfigurationBase, ReportElementDataBase, ReportElementModel, ReportElementType} from "app/shared/reports/elements/report-element-base.component";

@Component({
               selector   : "o3-report-element-alert-table",
               templateUrl: "./report-element-alert-table.component.html"
           })
export class ReportElementAlertTableComponent extends ReportElementBaseComponent<ReportElementAlertTableData, ReportElementAlertTableConfiguration>
{
    ranges: Models.RangeSelection[];

    async afterConfigurationChanges()
    {
        if (this.data)
        {
            this.ranges = [this.data.range];
            this.detectChanges();

            this.markAsComplete();
        }
    }
}

export class ReportElementAlertTableConfiguration extends ReportElementConfigurationBase
{
    public static newReportModel(label: string,
                                 element: Models.CustomReportElementAlertTable,
                                 range: Models.RangeSelection)
    {
        let model           = new ReportElementModel();
        model.type          = ReportElementType.AlertTable;
        model.configuration = new ReportElementAlertTableConfiguration();
        model.data          = new ReportElementAlertTableData(label, element, range);
        return model;
    }
}

export class ReportElementAlertTableData extends ReportElementDataBase
{
    constructor(readonly label: string,
                readonly config: AlertTableConfig,
                readonly range: Models.RangeSelection)
    {
        super();
    }

}
