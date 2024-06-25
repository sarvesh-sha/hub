import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {FormsModule} from "@angular/forms";
import {BrowserModule} from "@angular/platform-browser";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {RouterModule} from "@angular/router";
import {TreeModule} from "@circlon/angular-tree-component";
import {NgbModule} from "@ng-bootstrap/ng-bootstrap";
import {AngularDraggableModule} from "angular2-draggable";

import {ProvisionPageComponent} from "app/customer/provision/provision-page.component";
import {DomainModule} from "app/services/domain";
import {SharedModule} from "app/shared";

import {CdkModule} from "framework/cdk";
import {MaterialModule} from "framework/material";
import {FrameworkServicesModule} from "framework/services";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  ProvisionPageComponent
              ],
              imports     : [
                  BrowserModule,
                  CommonModule,
                  FormsModule,
                  RouterModule,
                  CdkModule,
                  MaterialModule,
                  FrameworkServicesModule,
                  FrameworkUIModule,
                  DomainModule,
                  SharedModule,
                  NgbModule,
                  TreeModule,
                  BrowserAnimationsModule,
                  AngularDraggableModule
              ]
          })
export class CustomerModule
{
}
