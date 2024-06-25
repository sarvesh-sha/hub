import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {ContextPaneModule} from "app/dashboard/context-pane";
import {ExperimentsColorPickerPageComponent} from "app/dashboard/experiments/exp-color-picker-page.component";
import {ExperimentsComponentsPageComponent} from "app/dashboard/experiments/exp-components-page.component";
import {ExperimentsContextPanePageComponent} from "app/dashboard/experiments/exp-context-pane-page.component";
import {ExperimentsControlPointSearchPageComponent} from "app/dashboard/experiments/exp-control-point-search-page.component";
import {ExperimentsFormsPageComponent} from "app/dashboard/experiments/exp-forms-page.component";
import {ExperimentsIconsPageComponent} from "app/dashboard/experiments/exp-icons-page.component";
import {ExperimentsMessagesPageComponent} from "app/dashboard/experiments/exp-messages-page.component";
import {ExperimentsRoutingModule} from "app/dashboard/experiments/experiments-routing.module";
import {TagConditionBuilderModule} from "app/shared/assets/tag-condition-builder/tag-condition-builder.module";
import {HierarchicalVisualizationModule} from "app/shared/charting/hierarchical-visualization/hierarchical-visualization.module";
import {ColorsModule} from "app/shared/colors/colors.module";
import {TimeRangeModule} from "app/shared/forms/time-range/time-range.module";
import {UnitEditorModule} from "app/shared/units/unit-editor.module";
import {FrameworkUIModule} from "framework/ui";
import {MarkdownModule} from "framework/ui/markdown/markdown.module";

@NgModule({
              declarations: [
                  ExperimentsColorPickerPageComponent,
                  ExperimentsComponentsPageComponent,
                  ExperimentsContextPanePageComponent,
                  ExperimentsControlPointSearchPageComponent,
                  ExperimentsFormsPageComponent,
                  ExperimentsIconsPageComponent,
                  ExperimentsMessagesPageComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  ContextPaneModule,
                  ColorsModule,
                  ExperimentsRoutingModule,
                  TagConditionBuilderModule,
                  TimeRangeModule,
                  HierarchicalVisualizationModule,
                  UnitEditorModule,
                  MarkdownModule
              ]
          })
export class ExperimentsModule {}
