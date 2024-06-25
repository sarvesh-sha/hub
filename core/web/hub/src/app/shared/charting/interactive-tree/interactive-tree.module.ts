import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {InteractiveTreeComponent} from "app/shared/charting/interactive-tree/interactive-tree.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [InteractiveTreeComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [InteractiveTreeComponent]
          })
export class InteractiveTreeModule {}
