import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {AssetGraphSelectorModule} from "app/shared/assets/asset-graph-selectors/asset-graph-selector.module";
import {AssetGraphStepModule} from "app/shared/assets/asset-graph-step/asset-graph-step.module";
import {MultipleGraphConfigurationModule} from "app/shared/assets/configuration/multiple-graph-configuration.module";
import {DataSourceWizardGraphBindingsStepComponent} from "app/shared/charting/data-source-wizard/data-source-wizard-graph-bindings-step.component";
import {DataSourceWizardGraphsStepComponent} from "app/shared/charting/data-source-wizard/data-source-wizard-graphs-step.component";
import {DataSourceWizardInteractionBehaviorStepComponent} from "app/shared/charting/data-source-wizard/data-source-wizard-interaction-behavior-step.component";
import {DataSourceWizardPivotTableStepComponent} from "app/shared/charting/data-source-wizard/data-source-wizard-pivot-table-step.component";
import {DataSourceWizardPointsStepComponent} from "app/shared/charting/data-source-wizard/data-source-wizard-points-step.component";
import {DataSourceWizardSourceTuplesStepComponent} from "app/shared/charting/data-source-wizard/data-source-wizard-source-tuples-step.component";
import {DataSourceWizardTypeStepComponent} from "app/shared/charting/data-source-wizard/data-source-wizard-type-step.component";
import {DataSourceWizardDialogComponent} from "app/shared/charting/data-source-wizard/data-source-wizard-dialog.component";
import {QueryColumnPickerComponent} from "app/shared/charting/data-source-wizard/query-column-picker.component";
import {InteractiveTreeModule} from "app/shared/charting/interactive-tree/interactive-tree.module";
import {ImagePreviewSelectorModule} from "app/shared/dropdowns/image-preview-selector/image-preview-selector.module";
import {InteractionHandlingModule} from "app/shared/interaction-handling/interaction-handling.module";
import {SearchModule} from "app/shared/search/search.module";
import {TablesModule} from "app/shared/tables/tables.module";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  DataSourceWizardDialogComponent,
                  DataSourceWizardGraphBindingsStepComponent,
                  DataSourceWizardGraphsStepComponent,
                  DataSourceWizardInteractionBehaviorStepComponent,
                  DataSourceWizardPointsStepComponent,
                  DataSourceWizardPivotTableStepComponent,
                  DataSourceWizardSourceTuplesStepComponent,
                  DataSourceWizardTypeStepComponent,
                  QueryColumnPickerComponent
              ],
              imports     : [
                  AssetGraphSelectorModule,
                  AssetGraphStepModule,
                  CommonModule,
                  FrameworkUIModule,
                  InteractionHandlingModule,
                  InteractiveTreeModule,
                  MultipleGraphConfigurationModule,
                  SearchModule,
                  TablesModule,
                  AssetGraphSelectorModule,
                  SearchModule,
                  ImagePreviewSelectorModule
              ],
              exports     : [DataSourceWizardDialogComponent]
          })
export class DataSourceWizardModule {}
