import {Component, ViewChild} from "@angular/core";

import {DataAggregationComponent, DataAggregationConfig} from "app/shared/aggregation/data-aggregation.component";
import {ReportElementBaseComponent, ReportElementConfigurationBase, ReportElementDataBase, ReportElementModel, ReportElementType} from "app/shared/reports/elements/report-element-base.component";

@Component({
               selector   : "o3-report-element-aggregation-table",
               templateUrl: "./report-element-aggregation-table.component.html"
           })
export class ReportElementAggregationTableComponent extends ReportElementBaseComponent<ReportElementAggregationTableData, ReportElementAggregationTableConfiguration>
{
    @ViewChild(DataAggregationComponent) private dataAggregation: DataAggregationComponent;

    configured = false;

    async afterConfigurationChanges()
    {
        if (this.data?.config && !this.configured)
        {
            this.configured = true;

            if (!this.dataAggregation) this.detectChanges();
            await this.dataAggregation.bind();

            this.markAsComplete();
        }
    }
}

export class ReportElementAggregationTableConfiguration extends ReportElementConfigurationBase
{
    public static newReportModel()
    {
        let model           = new ReportElementModel();
        model.type          = ReportElementType.AggregationTable;
        model.configuration = new ReportElementAggregationTableConfiguration();
        return model;
    }
}

export class ReportElementAggregationTableData extends ReportElementDataBase
{
    label: string;
    config: DataAggregationConfig;
}
