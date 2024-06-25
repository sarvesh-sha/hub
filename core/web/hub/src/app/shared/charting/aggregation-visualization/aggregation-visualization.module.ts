import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {AggregationTreeComponent} from "app/shared/charting/aggregation-visualization/aggregation-tree.component";
import {SourceChipModule} from "app/shared/charting/source-chip/source-chip.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  AggregationTreeComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  SourceChipModule
              ],
              exports     : [
                  AggregationTreeComponent
              ]
          })
export class AggregationVisualizationModule {}
