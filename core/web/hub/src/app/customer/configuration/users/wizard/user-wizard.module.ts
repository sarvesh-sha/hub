import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {UserWizardDialogComponent} from "app/customer/configuration/users/wizard/user-wizard-dialog.component";
import {UserWizardFieldsStep} from "app/customer/configuration/users/wizard/user-wizard-fields.step";
import {UserWizardInfoStep} from "app/customer/configuration/users/wizard/user-wizard-info.step";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  UserWizardDialogComponent,
                  UserWizardFieldsStep,
                  UserWizardInfoStep
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [
                  UserWizardDialogComponent
              ]
          })
export class UserWizardModule {}
