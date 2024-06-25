import {Component, Injector, ViewChild} from "@angular/core";
import * as Models from "app/services/proxy/model/models";
import {ReportElementBaseComponent, ReportElementConfigurationBase, ReportElementDataBase, ReportElementModel, ReportElementType} from "app/shared/reports/elements/report-element-base.component";
import {AggregationTrendChartComponent} from "framework/ui/charting/aggregation-trend-chart.component";
import {AggregationTrendGroup} from "framework/ui/charting/aggregation-trend-group";

@Component({
               selector   : "o3-report-element-aggregation-trend",
               styles     : [
                   ":host { display: block; width: 100%; }"
               ],
               templateUrl: "./report-element-aggregation-trend.component.html"
           })
export class ReportElementAggregationTrendComponent extends ReportElementBaseComponent<ReportElementAggregationTrendData, ReportElementAggregationTrendConfiguration>
{
    @ViewChild(AggregationTrendChartComponent, {static: true}) private aggregationTable: AggregationTrendChartComponent;
    configured = false;

    constructor(inj: Injector)
    {
        super(inj);
    }

    afterConfigurationChanges()
    {
        if (this.data?.results)
        {
            if (this.configured) return;
            this.configured = true;

            this.aggregationTable.refreshSize();
            this.markAsComplete();
        }
    }
}

export class ReportElementAggregationTrendConfiguration extends ReportElementConfigurationBase
{
    constructor(public label: string,
                public showY: boolean,
                public showLegend: boolean,
                public visualizationMode: Models.AggregationTrendVisualizationMode)
    {
        super();
    }

    static newReportModel(label: string,
                          showY: boolean,
                          showLegend: boolean,
                          visualizationMode: Models.AggregationTrendVisualizationMode)
    {
        let model           = new ReportElementModel();
        model.type          = ReportElementType.AggregationTrend;
        model.configuration = new ReportElementAggregationTrendConfiguration(label, showY, showLegend, visualizationMode);
        return model;
    }
}

export class ReportElementAggregationTrendData extends ReportElementDataBase
{
    results: AggregationTrendGroup[];
}
