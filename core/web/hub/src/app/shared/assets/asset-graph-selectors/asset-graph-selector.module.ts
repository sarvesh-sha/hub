import {NgModule} from "@angular/core";
import {AssetGraphNodeSelectorDirective} from "app/shared/assets/asset-graph-selectors/asset-graph-node-selector.directive";
import {AssetGraphSelectorDirective} from "app/shared/assets/asset-graph-selectors/asset-graph-selector.directive";

@NgModule({
              declarations: [
                  AssetGraphNodeSelectorDirective,
                  AssetGraphSelectorDirective
              ],
              imports     : [],
              exports     : [
                  AssetGraphNodeSelectorDirective,
                  AssetGraphSelectorDirective
              ]
          })
export class AssetGraphSelectorModule {}
