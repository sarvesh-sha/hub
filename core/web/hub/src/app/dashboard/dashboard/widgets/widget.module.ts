import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {ContextPaneModule} from "app/dashboard/context-pane";
import {WidgetContainerComponent} from "app/dashboard/dashboard/widgets/widget-container.component";
import {WidgetManagerComponent} from "app/dashboard/dashboard/widgets/widget-manager.component";
import {AggregationModule} from "app/shared/aggregation/aggregation.module";
import {AlertTableModule} from "app/shared/alerts/alert-table/alert-table.module";
import {AnnotationModule} from "app/shared/charting/annotations/annotation.module";
import {ChartingModule} from "app/shared/charting/charting.module";
import {SourceChipModule} from "app/shared/charting/source-chip/source-chip.module";
import {TimeSeriesChartModule} from "app/shared/charting/time-series-chart/time-series-chart.module";
import {AlertMapModule} from "app/shared/mapping/alert-map/alert-map.module";
import {TimelineModule} from "app/shared/timelines/timeline.module";
import {FrameworkUIModule} from "framework/ui";
import {LayoutHelpersModule} from "framework/ui/layout-helpers/layout-helpers.module";
import {RoutableViewModule} from "framework/ui/routable-view/routable-view.module";
import {SortModule} from "framework/ui/shared/sort/sort.module";

import {AggregationTableWidgetComponent} from "./aggregation-table-widget/widget.component";
import {AggregationTrendWidgetComponent} from "./aggregation-trend-widget/widget.component";
import {AggregationWidgetComponent} from "./aggregation-widget/widget.component";
import {AlertFeedWidgetComponent} from "./alert-feed-widget/widget.component";
import {AlertMapWidgetComponent} from "./alert-map-widget/widget.component";
import {AlertSummaryWidgetComponent} from "./alert-summary-widget/widget.component";
import {AlertTableWidgetComponent} from "./alert-table-widget/widget.component";
import {AssetGraphSelectorWidgetComponent} from "./asset-graph-selector-widget/widget.component";
import {ControlPointWidgetComponent} from "./control-point-widget/widget.component";
import {DeviceSummaryWidgetComponent} from "./device-summary-widget/widget.component";
import {GroupingWidgetComponent} from "./grouping-widget/widget.component";
import {ImageWidgetComponent} from "./image-widget/widget.component";
import {TextWidgetComponent} from "./text-widget/widget.component";
import {TimeSeriesWidgetComponent} from "./time-series-widget/widget.component";

@NgModule({
              declarations: [
                  AggregationTableWidgetComponent,
                  AggregationTrendWidgetComponent,
                  AggregationWidgetComponent,
                  AlertFeedWidgetComponent,
                  AlertMapWidgetComponent,
                  AlertSummaryWidgetComponent,
                  AlertTableWidgetComponent,
                  AssetGraphSelectorWidgetComponent,
                  ControlPointWidgetComponent,
                  DeviceSummaryWidgetComponent,
                  GroupingWidgetComponent,
                  ImageWidgetComponent,
                  TextWidgetComponent,
                  TimeSeriesWidgetComponent,
                  WidgetContainerComponent,
                  WidgetManagerComponent
              ],
              imports     : [
                  AggregationModule,
                  AlertTableModule,
                  AlertMapModule,
                  AnnotationModule,
                  ChartingModule,
                  CommonModule,
                  ContextPaneModule,
                  FrameworkUIModule,
                  LayoutHelpersModule,
                  RoutableViewModule,
                  SortModule,
                  SourceChipModule,
                  TimeSeriesChartModule,
                  TimelineModule
              ],
              exports     : [WidgetManagerComponent]
          })
export class WidgetModule {}
