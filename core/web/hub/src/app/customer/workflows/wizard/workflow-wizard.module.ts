import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {AssetLocationDialog} from "app/customer/workflows/asset-location-dialog";
import {AssetRenameDialog} from "app/customer/workflows/asset-rename-dialog";
import {AssetSamplingPeriodDialog} from "app/customer/workflows/asset-sampling-period-dialog";
import {AssignEquipmentDialog} from "app/customer/workflows/assign-equipment-dialog";
import {ChildEquipmentDialog} from "app/customer/workflows/child-equipment-dialog";
import {EquipmentClassModificationDialog} from "app/customer/workflows/equipment-class-modification-dialog";
import {EquipmentParentDialog} from "app/customer/workflows/equipment-parent-dialog";
import {LocationParentDialog} from "app/customer/workflows/location-parent-dialog";
import {PointClassModificationDialog} from "app/customer/workflows/point-class-modification-dialog";
import {WorkflowDetailsEditorComponent} from "app/customer/workflows/wizard/workflow-details-editor.component";
import {WorkflowWizardDescriptionStepComponent} from "app/customer/workflows/wizard/workflow-wizard-description-step.component";
import {WorkflowWizardDetailsStepComponent} from "app/customer/workflows/wizard/workflow-wizard-details-step.component";
import {WorkflowWizardTypeStepComponent} from "app/customer/workflows/wizard/workflow-wizard-type-step.component";
import {WorkflowWizardComponent} from "app/customer/workflows/wizard/workflow-wizard.component";
import {DirectivesModule} from "app/shared/directives/directives.module";
import {SelectorModule} from "app/shared/dropdowns/selector.module";
import {SearchModule} from "app/shared/search/search.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  WorkflowDetailsEditorComponent,
                  WorkflowWizardComponent,
                  WorkflowWizardDescriptionStepComponent,
                  WorkflowWizardDetailsStepComponent,
                  WorkflowWizardTypeStepComponent,

                  AssetLocationDialog,
                  AssetRenameDialog,
                  AssetSamplingPeriodDialog,
                  AssignEquipmentDialog,
                  ChildEquipmentDialog,
                  EquipmentClassModificationDialog,
                  EquipmentParentDialog,
                  LocationParentDialog,
                  PointClassModificationDialog
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  DirectivesModule,
                  SearchModule,
                  SelectorModule
              ],
              exports     : [
                  WorkflowWizardComponent,
                  WorkflowDetailsEditorComponent
              ]
          })
export class WorkflowWizardModule {}
