import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {UserDetailPageComponent} from "app/customer/configuration/users/user-detail-page.component";
import {UserListPageComponent} from "app/customer/configuration/users/user-list-page.component";
import {UserListComponent} from "app/customer/configuration/users/user-list.component";
import {UsersRoutingModule} from "app/customer/configuration/users/users-routing.module";
import {UserWizardModule} from "app/customer/configuration/users/wizard/user-wizard.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  UserListComponent,
                  UserListPageComponent,
                  UserDetailPageComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  UsersRoutingModule,
                  UserWizardModule
              ],
              exports     : [
                  UserListComponent
              ]
          })
export class UsersModule {}
