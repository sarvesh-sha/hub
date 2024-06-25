import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {CustomReportBuilderRowConfigurerComponent} from "app/customer/configuration/reports/custom-report-builder/custom-report-builder-row-configurer.component";
import {CustomReportBuilderFieldComponent} from "app/customer/configuration/reports/custom-report-builder/custom-report-builder-field.component";
import {CustomReportBuilderComponent} from "app/customer/configuration/reports/custom-report-builder/custom-report-builder.component";
import {CustomReportFieldWizardModule} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard.module";
import {ReportElementModule} from "app/shared/reports/elements/report-element.module";
import {UndoModule} from "app/shared/undo/undo.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  CustomReportBuilderRowConfigurerComponent,
                  CustomReportBuilderComponent,
                  CustomReportBuilderFieldComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  ReportElementModule,
                  CustomReportFieldWizardModule,
                  UndoModule
              ],
              exports     : [CustomReportBuilderComponent]
          })
export class CustomReportBuilderModule {}
