import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {AggregationModule} from "app/shared/aggregation/aggregation.module";
import {AlertTableModule} from "app/shared/alerts/alert-table/alert-table.module";
import {ChartingModule} from "app/shared/charting/charting.module";
import {TimeSeriesContainerModule} from "app/shared/charting/time-series-container/time-series-container.module";
import {ReportElementAggregatedValueComponent} from "app/shared/reports/elements/report-element-aggregated-value.component";
import {ReportElementAggregationTableComponent} from "app/shared/reports/elements/report-element-aggregation-table.component";
import {ReportElementAggregationTrendComponent} from "app/shared/reports/elements/report-element-aggregation-trend.component";
import {ReportElementAlertExecutionSummaryComponent} from "app/shared/reports/elements/report-element-alert-execution-summary.component";
import {ReportElementAlertFeedComponent} from "app/shared/reports/elements/report-element-alert-feed.component";
import {ReportElementAlertTableComponent} from "app/shared/reports/elements/report-element-alert-table.component";
import {ReportElementAlertsListComponent} from "app/shared/reports/elements/report-element-alerts-list.component";
import {ReportElementAlertsSummaryComponent} from "app/shared/reports/elements/report-element-alerts-summary.component";
import {ReportElementChartSetComponent} from "app/shared/reports/elements/report-element-chart-set.component";
import {ReportElementContainerComponent} from "app/shared/reports/elements/report-element-container.component";
import {ReportElementControlPointsListComponent} from "app/shared/reports/elements/report-element-control-points-list.component";
import {ReportElementDevicesListComponent} from "app/shared/reports/elements/report-element-devices-list.component";
import {ReportElementDevicesSummaryComponent} from "app/shared/reports/elements/report-element-devices-summary.component";
import {ReportElementEquipmentListComponent} from "app/shared/reports/elements/report-element-equipment-list.component";
import {ReportElementHeaderComponent} from "app/shared/reports/elements/report-element-header.component";
import {ReportElementRichTextComponent} from "app/shared/reports/elements/report-element-rich-text.component";
import {RichTextReportDateRangeComponent, RichTextReportDateTimeRangeComponent} from "app/shared/reports/elements/rich-text/rich-text-report-date-range.component";
import {RichTextReportDateSelectionDialogComponent} from "app/shared/reports/elements/rich-text/rich-text-report-date-selection-dialog.component";
import {TablesModule} from "app/shared/tables/tables.module";
import {TimelineModule} from "app/shared/timelines/timeline.module";

import {FrameworkUIModule} from "framework/ui";
import {MarkdownModule} from "framework/ui/markdown/markdown.module";

@NgModule({
              declarations: [
                  ReportElementAggregatedValueComponent,
                  ReportElementAggregationTableComponent,
                  ReportElementAggregationTrendComponent,
                  ReportElementAlertExecutionSummaryComponent,
                  ReportElementAlertFeedComponent,
                  ReportElementAlertTableComponent,
                  ReportElementAlertsListComponent,
                  ReportElementAlertsSummaryComponent,
                  ReportElementChartSetComponent,
                  ReportElementContainerComponent,
                  ReportElementControlPointsListComponent,
                  ReportElementDevicesListComponent,
                  ReportElementDevicesSummaryComponent,
                  ReportElementEquipmentListComponent,
                  ReportElementHeaderComponent,
                  ReportElementRichTextComponent,
                  RichTextReportDateRangeComponent,
                  RichTextReportDateTimeRangeComponent,
                  RichTextReportDateSelectionDialogComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  AggregationModule,
                  TimeSeriesContainerModule,
                  ChartingModule,
                  MarkdownModule,
                  TablesModule,
                  TimelineModule,
                  AlertTableModule
              ],
              exports     : [
                  ReportElementAggregatedValueComponent,
                  ReportElementAggregationTableComponent,
                  ReportElementAggregationTrendComponent,
                  ReportElementAlertExecutionSummaryComponent,
                  ReportElementAlertFeedComponent,
                  ReportElementAlertTableComponent,
                  ReportElementAlertsListComponent,
                  ReportElementAlertsSummaryComponent,
                  ReportElementChartSetComponent,
                  ReportElementContainerComponent,
                  ReportElementControlPointsListComponent,
                  ReportElementDevicesListComponent,
                  ReportElementDevicesSummaryComponent,
                  ReportElementEquipmentListComponent,
                  ReportElementHeaderComponent,
                  ReportElementRichTextComponent,
                  RichTextReportDateTimeRangeComponent,
                  RichTextReportDateSelectionDialogComponent
              ]
          })
export class ReportElementModule {}
