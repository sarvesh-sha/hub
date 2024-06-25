import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {UserGroupDetailPageComponent} from "app/customer/configuration/user-groups/user-group-detail-page.component";
import {UserGroupListPageComponent} from "app/customer/configuration/user-groups/user-group-list-page.component";
import {UserGroupsRoutingModule} from "app/customer/configuration/user-groups/user-groups-routing.module";
import {UserGroupWizardDialogComponent} from "app/customer/configuration/user-groups/wizard/user-group-wizard-dialog.component";
import {UserGroupWizardFieldsStep} from "app/customer/configuration/user-groups/wizard/user-group-wizard-fields-step.component";
import {UserGroupWizardInfoStep} from "app/customer/configuration/user-groups/wizard/user-group-wizard-info-step.component";
import {UsersModule} from "app/customer/configuration/users/users.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  UserGroupListPageComponent,
                  UserGroupDetailPageComponent,
                  UserGroupWizardDialogComponent,
                  UserGroupWizardFieldsStep,
                  UserGroupWizardInfoStep
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  UserGroupsRoutingModule,
                  UsersModule
              ]
          })
export class UserGroupsModule {}
