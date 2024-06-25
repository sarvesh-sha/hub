import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {SparkLineComponent} from "app/shared/charting/spark-line/spark-line.component";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [SparkLineComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [SparkLineComponent]
          })
export class SparkLineModule {}
