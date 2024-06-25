import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {ConsolidatedSourceChipComponent} from "app/shared/charting/source-chip/consolidated-source-chip.component";
import {SourceChipComponent} from "app/shared/charting/source-chip/source-chip.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  ConsolidatedSourceChipComponent,
                  SourceChipComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [
                  ConsolidatedSourceChipComponent,
                  SourceChipComponent
              ]
          })
export class SourceChipModule {}
