import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {DeviceElementsListModule} from "app/customer/device-elements/device-elements-list.module";
import {DevicesDetailPageComponent} from "app/customer/devices/devices-details-page/devices-detail-page.component";
import {DigineousDeviceLibraryComponent} from "app/customer/devices/devices-details-page/digineous/digineous-device-library.component";
import {DigineousWizardActionStep} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-action.step";
import {DigineousWizardActiveDevicesStep} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-active-devices.step";
import {DigineousWizardDeviceTemplatesActionStep} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-device-templates-action.step";
import {DigineousWizardDeviceTemplatesCopyStep} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-device-templates-copy.step";
import {DigineousWizardDeviceTemplatesCreateStep} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-device-templates-create.step";
import {DigineousWizardDeviceTemplatesDeleteStep} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-device-templates-delete.step";
import {DigineousWizardDeviceTemplatesEditStep} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-device-templates-edit.step";
import {DigineousWizardDialogComponent} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-dialog.component";
import {DigineousWizardImportStep} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-import.step";
import {DigineousWizardMachineTemplatesActionStep} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-machine-templates-action.step";
import {DigineousWizardMachineTemplatesCopyStep} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-machine-templates-copy.step";
import {DigineousWizardMachineTemplatesCreateStep} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-machine-templates-create.step";
import {DigineousWizardMachineTemplatesDeleteStep} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-machine-templates-delete.step";
import {DigineousWizardMachineTemplatesEditStep} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-machine-templates-edit.step";
import {DigineousWizardMachinesActionStep} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-machines-action.step";
import {DigineousWizardMachinesCreateStep} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-machines-create.step";
import {DigineousWizardMachinesDeleteStep} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-machines-delete.step";
import {DigineousWizardMachinesEditStep} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-machines-edit.step";
import {WorkflowWizardModule} from "app/customer/workflows/wizard/workflow-wizard.module";
import {BookmarkModule} from "app/shared/bookmarks/bookmark.module";
import {SelectorModule} from "app/shared/dropdowns/selector.module";
import {FilterModule} from "app/shared/filter/filter.module";
import {GaugeModule} from "app/shared/gauge/gauge.module";
import {TimelineModule} from "app/shared/timelines/timeline.module";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  DevicesDetailPageComponent,
                  DigineousDeviceLibraryComponent,
                  DigineousWizardActionStep,
                  DigineousWizardActiveDevicesStep,
                  DigineousWizardDeviceTemplatesActionStep,
                  DigineousWizardDeviceTemplatesCopyStep,
                  DigineousWizardDeviceTemplatesCreateStep,
                  DigineousWizardDeviceTemplatesDeleteStep,
                  DigineousWizardDeviceTemplatesEditStep,
                  DigineousWizardDialogComponent,
                  DigineousWizardImportStep,
                  DigineousWizardMachineTemplatesActionStep,
                  DigineousWizardMachineTemplatesCopyStep,
                  DigineousWizardMachineTemplatesCreateStep,
                  DigineousWizardMachineTemplatesDeleteStep,
                  DigineousWizardMachineTemplatesEditStep,
                  DigineousWizardMachinesActionStep,
                  DigineousWizardMachinesCreateStep,
                  DigineousWizardMachinesDeleteStep,
                  DigineousWizardMachinesEditStep
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  FilterModule,
                  SelectorModule,
                  GaugeModule,
                  TimelineModule,
                  DeviceElementsListModule,
                  BookmarkModule,
                  WorkflowWizardModule
              ],
              exports     : [
                  DevicesDetailPageComponent,
                  DigineousDeviceLibraryComponent
              ]
          })
export class DevicesDetailModule {}
