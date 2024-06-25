import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {AggregationGroupsComponent} from "app/shared/aggregation/aggregation-groups.component";
import {AggregationTableComponent} from "app/shared/aggregation/aggregation-table.component";
import {AggregationComponent} from "app/shared/aggregation/aggregation.component";
import {DataAggregationComponent} from "app/shared/aggregation/data-aggregation.component";
import {AggregationNumberFormatPipe, AggregationToLabelPipe} from "app/shared/aggregation/formatting/aggregation-format.pipe";
import {NodeBindingAggregationTable} from "app/shared/aggregation/node-binding-aggregation-table.component";
import {AggregationVisualizationModule} from "app/shared/charting/aggregation-visualization/aggregation-visualization.module";

import {FrameworkUIModule} from "framework/ui";
import {LayoutHelpersModule} from "framework/ui/layout-helpers/layout-helpers.module";
import {SortModule} from "framework/ui/shared/sort/sort.module";

@NgModule({
              declarations: [
                  AggregationComponent,
                  AggregationNumberFormatPipe,
                  AggregationGroupsComponent,
                  AggregationTableComponent,
                  AggregationToLabelPipe,
                  DataAggregationComponent,
                  NodeBindingAggregationTable
              ],
              imports     : [
                  AggregationVisualizationModule,
                  CommonModule,
                  FrameworkUIModule,
                  LayoutHelpersModule,
                  SortModule
              ],
              exports     : [
                  AggregationComponent,
                  AggregationNumberFormatPipe,
                  AggregationToLabelPipe,
                  DataAggregationComponent
              ]
          })
export class AggregationModule {}
