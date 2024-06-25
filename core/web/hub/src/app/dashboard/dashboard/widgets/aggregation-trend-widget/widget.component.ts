import {ChangeDetectionStrategy, Component, ViewChild} from "@angular/core";

import {WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {AggregationHelper} from "app/services/domain/aggregation.helper";
import {ClipboardEntryData} from "app/services/domain/clipboard.service";
import {ControlPointsGroupExtended, WidgetConfigurationExtended, WidgetDef} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";
import {Lookup, UtilsService} from "framework/services/utils.service";

import {AggregationTrendChartComponent} from "framework/ui/charting/aggregation-trend-chart.component";
import {AggregationTrendGroup} from "framework/ui/charting/aggregation-trend-group";

@Component({
               selector       : "o3-aggregation-trend-widget",
               templateUrl    : "./widget.template.html",
               styleUrls      : ["./widget.styles.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AggregationTrendWidgetComponent extends WidgetBaseComponent<Models.AggregationTrendWidgetConfiguration, AggregationTrendWidgetConfigurationExtended>
{
    private m_chart: AggregationTrendChartComponent;
    @ViewChild(AggregationTrendChartComponent) set chart(chart: AggregationTrendChartComponent)
    {
        this.m_chart = chart;
        this.refresh();
    }

    get test_chart(): AggregationTrendChartComponent
    {
        return this.m_chart;
    }

    renderableResults: AggregationTrendGroup[] = [];

    public async bind(): Promise<void>
    {
        await super.bind();

        if (!this.config.name) this.config.name = "Aggregation Trend";

        await this.getResults();
    }

    private async getResults()
    {
        this.renderableResults = await AggregationHelper.getAggregationTrendGroups(this.app, this.config.filterableRange, this.config.granularity, this.config.groups);
    }

    public async refreshSize(): Promise<boolean>
    {
        return !!this.m_chart?.refreshSize();
    }

    public async refreshContent(): Promise<void>
    {
        await super.refreshContent();
        await this.getResults();
    }

    protected getClipboardData(): ClipboardEntryData<Models.AggregationTrendWidgetConfiguration, Models.ReportLayoutItem>
    {
        let model        = Models.AggregationTrendWidgetConfiguration.deepClone(this.config);
        let sharedGraphs = ControlPointsGroupExtended.getLocalGraphs(model.groups);

        return new class extends ClipboardEntryData<Models.AggregationTrendWidgetConfiguration, Models.ReportLayoutItem>
        {
            constructor()
            {
                super("aggregation trend");
            }

            public getDashboardWidget(): Models.AggregationTrendWidgetConfiguration
            {
                return Models.AggregationTrendWidgetConfiguration.deepClone(model);
            }

            public getReportItem(oldToNewGraphId: Lookup<string>): Models.ReportLayoutItem
            {
                let groups = ClipboardEntryData.getReportGroups(model.groups, sharedGraphs, oldToNewGraphId);
                if (model.groups.length !== groups.length) return null;

                let element = Models.CustomReportElementAggregationTrend.deepClone({
                                                                                       label            : model.name,
                                                                                       groups           : groups,
                                                                                       granularity      : model.granularity,
                                                                                       visualizationMode: model.visualizationMode,
                                                                                       showY            : model.showY,
                                                                                       showLegend       : model.showLegend
                                                                                   });
                return Models.ReportLayoutItem.newInstance({element: element});
            }

            public getReportGraphs(): Models.SharedAssetGraph[]
            {
                return UtilsService.arrayCopy(sharedGraphs);
            }
        }();
    }
}


@WidgetDef({
               friendlyName      : "Aggregation Trend",
               typeName          : "AGGREGATION_TREND",
               model             : Models.AggregationTrendWidgetConfiguration,
               component         : AggregationTrendWidgetComponent,
               dashboardCreatable: true,
               subgroupCreatable : true,
               maximizable       : true,
               defaultWidth      : 6,
               defaultHeight     : 4,
               hostScalableText  : false,
               needsProtector    : false,
               documentation     : {
                   description: "The Aggregation Trend widget allows you to specify a set of control points and a type of aggregation and then select a period of time to show data for. Aggregations are calculated at the selected interval over the period specified and then plotted across time as either bars or a line.",
                   examples   : [
                       {
                           file       : "widgets/AGGREGATION_TREND/bar.png",
                           label      : "Bar Chart",
                           description: "Maxes of 2 temperature control point groups for the past 7 days, calculated with a 1 day interval."
                       },
                       {
                           file       : "widgets/AGGREGATION_TREND/line.png",
                           label      : "Line Chart",
                           description: "Aggregations on 4 groups of control points for the past 7 days, calculated with a 1 day interval."
                       }
                   ]
               }

           })
export class AggregationTrendWidgetConfigurationExtended extends WidgetConfigurationExtended<Models.AggregationTrendWidgetConfiguration>
{
    protected initializeForWizardInner()
    {
        const model = this.model;

        const group = ControlPointsGroupExtended.newModel({selections: Models.ControlPointsSelection.newInstance({identities: []})});

        model.groups            = [group];
        model.filterableRange   = Models.FilterableTimeRange.newInstance({range: RangeSelectionExtended.newModel(Models.TimeRangeId.Last30Days)});
        model.showY             = true;
        model.granularity       = Models.AggregationGranularity.Week;
        model.visualizationMode = Models.AggregationTrendVisualizationMode.Line;
    }

    public getBindings(): Models.AssetGraphBinding[]
    {
        return [];
    }
}
