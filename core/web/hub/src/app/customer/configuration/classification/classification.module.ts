import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {ClassificationDetailPageComponent} from "app/customer/configuration/classification/classification-detail-page.component";
import {ClassificationDiffComponent, ClassificationDiffPickerDialogComponent, ClassificationDiffTableComponent} from "app/customer/configuration/classification/classification-diff/classification-diff.component";
import {LogicDiffComponent} from "app/customer/configuration/classification/classification-diff/logic-diff.component";
import {ClassificationOverrideDialogComponent} from "app/customer/configuration/classification/classification-override-dialog.component";
import {ClassificationOverridesComponent} from "app/customer/configuration/classification/classification-overrides.component";
import {ClassificationRoutingModule} from "app/customer/configuration/classification/classification-routing.module";
import {ClassificationRulesClassEditorComponent} from "app/customer/configuration/classification/classification-rules-class-editor.component";
import {ClassificationRulesEditorComponent} from "app/customer/configuration/classification/classification-rules-editor.component";
import {ClassificationRulesListComponent} from "app/customer/configuration/classification/classification-rules-list.component";
import {ClassificationRunComponent} from "app/customer/configuration/classification/classification-run/classification-run.component";
import {ClassificationSummaryPageComponent} from "app/customer/configuration/classification/classification-summary-page.component";
import {ClassificationTestFormComponent} from "app/customer/configuration/classification/classification-test-form.component";
import {ClassificationTestComponent} from "app/customer/configuration/classification/classification-test.component";
import {ClassificationValidationRulesEditorComponent} from "app/customer/configuration/classification/classification-validation-rules-editor.component";
import {DeviceElementLinkComponent} from "app/customer/configuration/classification/device-element-link.component";
import {DeviceElementsListValidationComponent} from "app/customer/configuration/classification/device-elements-list-validation.component";
import {EquipmentExplorerComponent} from "app/customer/configuration/classification/equipment-explorer.component";
import {ValidationRunComponent} from "app/customer/configuration/classification/validation-run.component";
import {CommonConfigurationModule} from "app/customer/configuration/common/common.module";
import {DeviceElementsListModule} from "app/customer/device-elements/device-elements-list.module";
import {EnginesModule} from "app/customer/engines/engines.module";
import {EquipmentListModule} from "app/customer/equipment/equipment-list.module";
import {SelectorModule} from "app/shared/dropdowns/selector.module";
import {SearchModule} from "app/shared/search/search.module";
import {TablesModule} from "app/shared/tables/tables.module";
import {PreviewInvokerModule} from "app/shared/utils/preview-invoker/preview-invoker.module";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  ClassificationDiffComponent,
                  ClassificationDiffPickerDialogComponent,
                  ClassificationDiffTableComponent,
                  ClassificationRulesListComponent,
                  ClassificationSummaryPageComponent,
                  ClassificationDetailPageComponent,
                  ClassificationOverridesComponent,
                  ClassificationRunComponent,
                  ClassificationRulesEditorComponent,
                  ClassificationRulesClassEditorComponent,
                  ClassificationTestComponent,
                  ClassificationTestFormComponent,
                  ClassificationOverrideDialogComponent,
                  ClassificationValidationRulesEditorComponent,
                  DeviceElementsListValidationComponent,
                  DeviceElementLinkComponent,
                  EquipmentExplorerComponent,
                  LogicDiffComponent,
                  ValidationRunComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  DeviceElementsListModule,
                  EquipmentListModule,
                  EnginesModule,
                  CommonConfigurationModule,
                  ClassificationRoutingModule,
                  SelectorModule,
                  TablesModule,
                  PreviewInvokerModule,
                  SearchModule
              ]
          })
export class ClassificationModule {}
