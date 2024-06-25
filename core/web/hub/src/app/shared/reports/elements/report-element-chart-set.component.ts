import {Component, Injector} from "@angular/core";

import * as Models from "app/services/proxy/model/models";
import {ReportElementBaseComponent, ReportElementConfigurationBase, ReportElementDataBase, ReportElementModel, ReportElementType} from "app/shared/reports/elements/report-element-base.component";

@Component({
               selector   : "o3-report-element-chart-set",
               templateUrl: "./report-element-chart-set.component.html"
           })
export class ReportElementChartSetComponent extends ReportElementBaseComponent<ReportElementChartSetData, ReportElementChartSetConfiguration>
{
    constructor(inj: Injector)
    {
        super(inj);
    }

    async afterConfigurationChanges()
    {
    }
}

export class ReportElementChartSetData extends ReportElementDataBase
{
    range: Models.RangeSelection;
    charts: Models.TimeSeriesChartConfiguration[];
}

export class ReportElementChartSetConfiguration extends ReportElementConfigurationBase
{
    public static newReportModel()
    {
        let model           = new ReportElementModel();
        model.type          = ReportElementType.ChartSet;
        model.configuration = new ReportElementChartSetConfiguration();
        return model;
    }
}
