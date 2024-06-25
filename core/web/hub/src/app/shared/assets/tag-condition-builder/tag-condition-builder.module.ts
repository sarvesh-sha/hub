import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {TagConditionBuilderComponent} from "app/shared/assets/tag-condition-builder/tag-condition-builder.component";
import {TagConditionNodeComponent} from "app/shared/assets/tag-condition-builder/tag-condition-node.component";
import {SelectorModule} from "app/shared/dropdowns/selector.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  TagConditionBuilderComponent,
                  TagConditionNodeComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  SelectorModule
              ],
              exports     : [TagConditionBuilderComponent]
          })
export class TagConditionBuilderModule {}
