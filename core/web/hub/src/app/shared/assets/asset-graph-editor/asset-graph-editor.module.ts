import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {AssetGraphEditorComponent, AssetGraphMatchesListComponent} from "app/shared/assets/asset-graph-editor/asset-graph-editor.component";
import {TagConditionBuilderModule} from "app/shared/assets/tag-condition-builder/tag-condition-builder.module";
import {SelectorModule} from "app/shared/dropdowns/selector.module";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  AssetGraphEditorComponent,
                  AssetGraphMatchesListComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  TagConditionBuilderModule,
                  SelectorModule
              ],
              exports     : [AssetGraphEditorComponent]
          })
export class AssetGraphEditorModule {}
