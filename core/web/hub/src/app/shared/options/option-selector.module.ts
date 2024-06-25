import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {TextAlignmentSelectorComponent} from "app/shared/options/text-alignment-selector.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [TextAlignmentSelectorComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [TextAlignmentSelectorComponent]
          })
export class OptionSelectorModule {}
