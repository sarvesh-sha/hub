import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {FormsModule} from "@angular/forms";
import {TreeModule} from "@circlon/angular-tree-component";
import {NgbModule} from "@ng-bootstrap/ng-bootstrap";

import {DomainModule} from "app/services/domain";

import {AppNavigationResolver} from "app/shared/app-navigation.resolver";
import {CdkModule} from "framework/cdk";

import {MaterialModule} from "framework/material";
import {FrameworkServicesModule} from "framework/services";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              imports  : [
                  CommonModule,
                  FormsModule,
                  MaterialModule,
                  FrameworkServicesModule,
                  FrameworkUIModule,
                  CdkModule,
                  DomainModule,
                  NgbModule,
                  TreeModule
              ],
              providers: [
                  AppNavigationResolver
              ]
          })
export class SharedModule
{
}
