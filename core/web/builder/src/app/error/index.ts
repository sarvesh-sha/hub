import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {ErrorPageComponent} from "app/error/error-page.component";
import {NotFoundPageComponent} from "app/error/not-found-page.component";

import {MaterialModule} from "framework/material";
import {FrameworkServicesModule} from "framework/services";

@NgModule({
              declarations: [
                  ErrorPageComponent,
                  NotFoundPageComponent
              ],
              imports     : [
                  CommonModule,
                  MaterialModule,
                  FrameworkServicesModule
              ]
          })
export class ErrorModule {}
