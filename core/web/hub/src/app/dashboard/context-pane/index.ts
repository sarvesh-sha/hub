import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {DynamicPaneFieldComponent} from "app/dashboard/context-pane/fields/dynamic-pane-field.component";
import {PaneAlertCountFieldComponent} from "app/dashboard/context-pane/fields/pane-alert-count-field.component";
import {PaneAlertFeedFieldComponent} from "app/dashboard/context-pane/fields/pane-alert-feed-field.component";
import {PaneChartFieldComponent} from "app/dashboard/context-pane/fields/pane-chart-field.component";
import {PaneControlPointAggregationFieldComponent} from "app/dashboard/context-pane/fields/pane-control-point-aggregation-field.component";
import {PaneControlPointCurrentValueFieldComponent} from "app/dashboard/context-pane/fields/pane-control-point-current-value-field.component";
import {PaneGaugeFieldComponent} from "app/dashboard/context-pane/fields/pane-gauge-field.component";
import {PanePathMapFieldComponent} from "app/dashboard/context-pane/fields/pane-path-map-field.component";
import {ContextPaneCardComponent} from "app/dashboard/context-pane/panes/context-pane-card.component";
import {ContextPaneFieldComponent} from "app/dashboard/context-pane/panes/fields/context-pane-field.component";
import {ContextPaneComponent, ContextPaneHeaderDirective} from "app/dashboard/context-pane/panes/context-pane.component";
import {DynamicPaneComponent} from "app/dashboard/context-pane/panes/dynamic-pane.component";
import {AggregationModule} from "app/shared/aggregation/aggregation.module";
import {TimeSeriesChartModule} from "app/shared/charting/time-series-chart/time-series-chart.module";
import {TimeRangeModule} from "app/shared/forms/time-range/time-range.module";
import {GaugeModule} from "app/shared/gauge/gauge.module";
import {GpsMapModule} from "app/shared/mapping/gps-map/gps-map.module";
import {TimelineModule} from "app/shared/timelines/timeline.module";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  ContextPaneCardComponent,
                  ContextPaneComponent,
                  ContextPaneFieldComponent,
                  ContextPaneHeaderDirective,
                  DynamicPaneComponent,
                  DynamicPaneFieldComponent,
                  PaneAlertCountFieldComponent,
                  PaneAlertFeedFieldComponent,
                  PaneChartFieldComponent,
                  PaneControlPointCurrentValueFieldComponent,
                  PaneControlPointAggregationFieldComponent,
                  PaneGaugeFieldComponent,
                  PanePathMapFieldComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  AggregationModule,
                  TimelineModule,
                  TimeRangeModule,
                  TimeSeriesChartModule,
                  GaugeModule,
                  GpsMapModule
              ],
              exports     : [
                  ContextPaneCardComponent,
                  ContextPaneComponent,
                  ContextPaneFieldComponent,
                  ContextPaneHeaderDirective,
                  DynamicPaneComponent,
                  DynamicPaneFieldComponent
              ]
          })
export class ContextPaneModule
{
}
