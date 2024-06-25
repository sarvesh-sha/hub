import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {FormsModule} from "@angular/forms";
import {TreeModule} from "@circlon/angular-tree-component";

import {DomainModule} from "app/services/domain";
import {UIModule} from "app/services/ui";

import {HasAccessDirective} from "app/shared/directives/has-access.directive";
import {IfSystemPreferenceDirective} from "app/shared/directives/if-system-preference.directive";
import {CdkModule} from "framework/cdk";

import {MaterialModule} from "framework/material";
import {FrameworkServicesModule} from "framework/services";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations   : [
                  HasAccessDirective,
                  IfSystemPreferenceDirective
              ],
              imports        : [
                  CommonModule,
                  FormsModule,
                  MaterialModule,
                  FrameworkServicesModule,
                  FrameworkUIModule,
                  CdkModule,
                  DomainModule,
                  UIModule,
                  TreeModule
              ],
              exports        : [
                  HasAccessDirective
              ]
          })
export class SharedModule {}
