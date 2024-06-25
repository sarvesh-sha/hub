import {AppContext} from "app/app.service";
import {TimeSeriesChartConfigurationExtended, TimeSeriesChartHandler} from "app/customer/visualization/time-series-utils";
import {AggregationHelper} from "app/services/domain/aggregation.helper";
import {AssetGraphResponseHolder, SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import {ReportDefinitionVersionExtended} from "app/services/domain/report-definition-versions.service";
import {ReportDefinitionDetailsExtended, ReportDefinitionExtended} from "app/services/domain/report-definitions.service";
import {ReportConfigurationExtended} from "app/services/domain/reporting.service";
import {ControlPointsGroupExtended} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {DataAggregationConfig} from "app/shared/aggregation/data-aggregation.component";
import {ReportElementAggregatedValueConfiguration, ReportElementAggregatedValueData} from "app/shared/reports/elements/report-element-aggregated-value.component";
import {ReportElementAggregationTableConfiguration, ReportElementAggregationTableData} from "app/shared/reports/elements/report-element-aggregation-table.component";
import {ReportElementAggregationTrendConfiguration, ReportElementAggregationTrendData} from "app/shared/reports/elements/report-element-aggregation-trend.component";
import {ReportElementAlertExecutionSummaryConfiguration, ReportElementAlertExecutionSummaryData} from "app/shared/reports/elements/report-element-alert-execution-summary.component";
import {ReportElementAlertFeedConfiguration} from "app/shared/reports/elements/report-element-alert-feed.component";
import {ReportElementAlertTableConfiguration} from "app/shared/reports/elements/report-element-alert-table.component";
import {ReportElementModel, ReportSection, ReportSectionBuilder} from "app/shared/reports/elements/report-element-base.component";
import {ReportElementChartSetConfiguration, ReportElementChartSetData} from "app/shared/reports/elements/report-element-chart-set.component";
import {ReportElementControlPointsListConfiguration, ReportElementControlPointsListData} from "app/shared/reports/elements/report-element-control-points-list.component";
import {ReportElementPageBreakConfiguration} from "app/shared/reports/elements/report-element-page-break.component";
import {ReportElementRichTextConfiguration} from "app/shared/reports/elements/report-element-rich-text.component";

import {Future, inParallel, mapInParallel} from "framework/utils/concurrency";
import moment from "framework/utils/moment";

export class DynamicReport
{
    // Set through decorator
    protected m_report: ReportDefinitionDetailsExtended;

    protected m_data: ReportConfigurationExtended;

    protected m_sections: ReportSection[];
    protected m_progress: number = 0;

    constructor(protected m_definition: ReportDefinitionExtended,
                protected m_version: ReportDefinitionVersionExtended,
                protected m_domain: AppDomainContext)
    {
        if (!m_version || !m_definition)
        {
            this.m_version    = m_domain.reportDefinitionVersions.allocateInstance();
            this.m_definition = m_domain.reportDefinitions.allocateInstance();
            this.m_report     = new ReportDefinitionDetailsExtended(m_domain, new Models.ReportDefinitionDetails());
        }
        else
        {
            this.m_report = m_version.getDetailsExtended();
        }

        this.m_data = this.m_report.getOrCreateConfig();
    }

    public getData(): ReportConfigurationExtended
    {
        return this.m_data;
    }

    public setData(config: ReportConfigurationExtended)
    {
        this.m_data = config;
        this.synchronize();
    }

    public synchronize()
    {
        this.m_report.typedModel.reportConfiguration = this.m_data.model;
    }

    public getDetails(): ReportDefinitionDetailsExtended
    {
        this.synchronize();
        return this.m_report;
    }

    public static create(domain: AppDomainContext,
                         definition: ReportDefinitionExtended,
                         version: ReportDefinitionVersionExtended): DynamicReport
    {
        return new DynamicReport(definition, version, domain);
    }

    public async startReport(app: AppContext,
                             comp: SharedSvc.BaseComponentWithTables,
                             rangeStart: moment.Moment,
                             rangeEnd: moment.Moment)
    {
        let graphs = await SharedAssetGraphExtended.loadGraphs(this.m_domain, this.m_data.sharedGraphs);

        let sections = [];
        for (let row of this.m_data.container.children)
        {
            let section = await this.buildRow(row, app, graphs, rangeStart, rangeEnd);

            sections.push(section);
        }

        this.m_sections = sections;
        this.m_progress = 100;
    }

    private async buildRow(layout: Models.ReportLayoutBase,
                           app: AppContext,
                           graphMap: Map<string, SharedAssetGraphExtended>,
                           rangeStart: moment.Moment,
                           rangeEnd: moment.Moment): Promise<ReportSection>
    {
        let sectionBuilder = new ReportSectionBuilder();
        if (layout instanceof Models.ReportLayoutItem)
        {
            sectionBuilder.withColumn(layout, await DynamicReport.getModel(app, layout, graphMap, rangeStart, rangeEnd, this));
            return sectionBuilder.build();
        }

        for (let child of layout.children || [])
        {
            if (child instanceof Models.ReportLayoutItem)
            {
                sectionBuilder.withColumn(child, await DynamicReport.getModel(app, child, graphMap, rangeStart, rangeEnd, this));
            }
            else
            {
                let children = await mapInParallel(child.children, async (item) =>
                {
                    if (item instanceof Models.ReportLayoutItem)
                    {
                        return DynamicReport.getModel(app, item, graphMap, rangeStart, rangeEnd, this);
                    }
                    else
                    {
                        return this.buildRow(item, app, graphMap, rangeStart, rangeEnd);
                    }
                });
                sectionBuilder.withColumn(child, ...children);
            }
        }

        return sectionBuilder.build();
    }

    public checkProgress()
    {
        return {
            progress: this.m_progress,
            results : this.m_sections
        };
    }

    public static getGraphResponse(graphMap: Map<string, SharedAssetGraphExtended>,
                                   binding: Models.AssetGraphBinding): Promise<AssetGraphResponseHolder>
    {
        if (!binding) return null;
        let assetGraph = graphMap.get(binding.graphId);
        return assetGraph?.resolve();
    }

    public static async getModel(app: AppContext,
                                 item: Models.ReportLayoutItem,
                                 graphMap: Map<string, SharedAssetGraphExtended>,
                                 rangeStart: moment.Moment,
                                 rangeEnd: moment.Moment,
                                 report?: DynamicReport): Promise<ReportElementModel>
    {
        let range           = Models.RangeSelection.newInstance({
                                                                    start: rangeStart.toDate(),
                                                                    end  : rangeEnd.toDate()
                                                                });
        let filterableRange = Models.FilterableTimeRange.newInstance({range: range});

        let element                          = item.element;
        let elementModel: ReportElementModel = null;
        if (element instanceof Models.CustomReportElementDeviceElementList)
        {
            if (!element.pointInput) return elementModel;

            let graphResponses = await this.getGraphResponse(graphMap, element.pointInput);
            let identities     = graphResponses.resolveIdentities(element.pointInput);
            let controlPoints  = await app.domain.assets.getTypedExtendedBatch(DeviceElementExtended, identities);
            elementModel       = ReportElementControlPointsListConfiguration.newReportModel();
            elementModel.data  = ReportElementControlPointsListData.newInstance(controlPoints, rangeStart, rangeEnd);
        }
        else if (element instanceof Models.CustomReportElementAggregatedValue)
        {
            if (!element.controlPointGroup || !element.controlPointGroup.unitsFactors) return elementModel;

            if (element.controlPointGroup.pointInput)
            {
                let graphResponses                   = await this.getGraphResponse(graphMap, element.controlPointGroup.pointInput);
                let identities                       = graphResponses.resolveIdentities(element.controlPointGroup.pointInput);
                element.controlPointGroup.selections = Models.ControlPointsSelection.newInstance({identities: identities});
            }

            filterableRange.filter          = element.filter;
            filterableRange.isFilterApplied = element.isFilterApplied;

            elementModel           = ReportElementAggregatedValueConfiguration.newReportModel();
            let data               = new ReportElementAggregatedValueData();
            data.label             = element.label;
            data.controlPointGroup = element.controlPointGroup;
            data.hideRange         = true;
            data.filterableRange   = filterableRange;
            elementModel.data      = data;
        }
        else if (element instanceof Models.CustomReportElementAggregationTable)
        {
            if (!element.visualizationMode) return elementModel;
            if (!element.controlPointDisplayType) return elementModel;
            if (element.groups.some((g) => !ControlPointsGroupExtended.isValid(g, true, true, true, true))) return elementModel;

            for (let group of element.groups)
            {
                if (group.pointInput && !group.graph)
                {
                    let graphResponses = await this.getGraphResponse(graphMap, group.pointInput);
                    let identities     = graphResponses?.resolveIdentities(group.pointInput);
                    group.selections   = Models.ControlPointsSelection.newInstance({identities: identities || []});
                }
            }

            let config: DataAggregationConfig = {
                groups                 : element.groups,
                columns                : element.columns,
                graph                  : graphMap.get(element.graphId)?.model,
                filterableRanges       : [filterableRange],
                controlPointDisplayType: element.controlPointDisplayType,
                visualizationMode      : element.visualizationMode,
                initialSort            : element.initialSort,
                isolateGroupRanges     : element.isolateGroupRanges,
                visualizationLegend    : true,
                visualizationRanges    : false
            };

            elementModel      = ReportElementAggregationTableConfiguration.newReportModel();
            let data          = new ReportElementAggregationTableData();
            data.config       = config;
            data.label        = element.label;
            elementModel.data = data;
        }
        else if (element instanceof Models.CustomReportElementAggregationTrend)
        {
            if (!element.granularity) return elementModel;
            if (!element.visualizationMode) return elementModel;
            if (element.groups.some((g) => !ControlPointsGroupExtended.isValid(g, false, true, true, true))) return elementModel;

            for (let group of element.groups)
            {
                if (group.pointInput)
                {
                    let graphResponses = await this.getGraphResponse(graphMap, group.pointInput);
                    let identities     = graphResponses.resolveIdentities(group.pointInput);
                    group.selections   = Models.ControlPointsSelection.newInstance({identities: identities});
                }
            }

            elementModel      = ReportElementAggregationTrendConfiguration.newReportModel(element.label, element.showY, element.showLegend, element.visualizationMode);
            let data          = new ReportElementAggregationTrendData();
            data.results      = await AggregationHelper.getAggregationTrendGroups(app, filterableRange, element.granularity, element.groups);
            elementModel.data = data;
        }
        else if (element instanceof Models.CustomReportElementAlertFeed)
        {
            elementModel = ReportElementAlertFeedConfiguration.newReportModel(element.label, rangeStart, rangeEnd, element.locations, element.alertTypes);
        }
        else if (element instanceof Models.CustomReportElementAlertTable)
        {
            elementModel = ReportElementAlertTableConfiguration.newReportModel(element.label, element, filterableRange.range);
        }
        else if (element instanceof Models.CustomReportElementChartSet)
        {
            let range    = Models.RangeSelection.newInstance({
                                                                 start: rangeStart.toDate(),
                                                                 end  : rangeEnd.toDate()
                                                             });
            elementModel = ReportElementChartSetConfiguration.newReportModel();
            let data     = new ReportElementChartSetData();
            data.range   = range;
            data.charts  = [];
            await inParallel(element.charts, async (chart) =>
            {
                let chartExt = await TimeSeriesChartConfigurationExtended.newInstance(app, chart);
                if (TimeSeriesChartHandler.newInstance(chartExt)
                                          .hasSources())
                {
                    data.charts.push(chart);
                }
            });
            elementModel.data = data;
        }
        else if (element instanceof Models.CustomReportElementRichText)
        {
            elementModel = ReportElementRichTextConfiguration.newReportModel(element.data, element.backgroundColor);
        }
        else if (element instanceof Models.CustomReportElementPageBreak)
        {
            elementModel = ReportElementPageBreakConfiguration.newReportModel();
        }
        else if (element instanceof Models.CustomReportElementAlertExecution)
        {
            let alertDefinition                                   = await app.domain.alertDefinitions.getExtendedByIdentity(element.definition);
            let alertVersion                                      = await app.domain.alertDefinitionVersions.getExtendedByIdentity(element.version);
            elementModel                                          = ReportElementAlertExecutionSummaryConfiguration.newReportModel();
            let alertDataChecker                                  = await ReportElementAlertExecutionSummaryData.newInstance(alertDefinition, alertVersion, rangeStart, rangeEnd);
            let alertData: ReportElementAlertExecutionSummaryData = null;
            while (!alertData)
            {
                await Future.delayed(2000);
                let {
                        progress,
                        results
                    }     = await alertDataChecker();
                alertData = results;
                if (report) report.m_progress = progress;
            }

            alertData.range   = range;
            elementModel.data = alertData;
        }
        elementModel.configuration.widthRatio = item.widthRatio || 1;

        return elementModel;
    }
}
