import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {AssetGraphEditorModule} from "app/shared/assets/asset-graph-editor/asset-graph-editor.module";
import {AssetGraphSelectorModule} from "app/shared/assets/asset-graph-selectors/asset-graph-selector.module";
import {AssetGraphStepComponent} from "app/shared/assets/asset-graph-step/asset-graph-step.component";
import {UndoModule} from "app/shared/undo/undo.module";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [AssetGraphStepComponent],
              imports     : [
                  AssetGraphEditorModule,
                  AssetGraphSelectorModule,
                  CommonModule,
                  FrameworkUIModule,
                  UndoModule
              ],
              exports     : [AssetGraphStepComponent]
          })
export class AssetGraphStepModule {}
