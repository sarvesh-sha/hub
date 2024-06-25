import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {CommonConfigurationModule} from "app/customer/configuration/common/common.module";
import {CustomReportBuilderModule} from "app/customer/configuration/reports/custom-report-builder/custom-report-builder.module";
import {ReportPreviewButtonComponent} from "app/customer/configuration/reports/wizard/report-preview-button.component";
import {ReportPreviewDialogComponent} from "app/customer/configuration/reports/wizard/report-preview-dialog.component";
import {ReportWizardCustomStepComponent} from "app/customer/configuration/reports/wizard/report-wizard-custom-step.component";
import {ReportWizardDeliveryStepComponent} from "app/customer/configuration/reports/wizard/report-wizard-delivery-step.component";
import {ReportWizardDialogComponent} from "app/customer/configuration/reports/wizard/report-wizard-dialog.component";
import {ReportWizardPageSettingsStepComponent} from "app/customer/configuration/reports/wizard/report-wizard-page-settings-step.component";
import {ReportWizardNameStepComponent} from "app/customer/configuration/reports/wizard/report-wizard-name-step.component";
import {DayPickerModule} from "app/shared/forms/day-picker/day-picker.module";
import {TimeRangeModule} from "app/shared/forms/time-range/time-range.module";
import {MultipleGraphConfigurationModule} from "app/shared/assets/configuration/multiple-graph-configuration.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  ReportWizardDialogComponent,
                  ReportWizardCustomStepComponent,
                  ReportWizardDeliveryStepComponent,
                  ReportWizardPageSettingsStepComponent,
                  ReportWizardNameStepComponent,
                  ReportPreviewButtonComponent,
                  ReportPreviewDialogComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  MultipleGraphConfigurationModule,
                  CustomReportBuilderModule,
                  DayPickerModule,
                  CommonConfigurationModule,
                  TimeRangeModule
              ],
              exports     : [ReportWizardDialogComponent]
          })
export class ReportWizardModule {}
