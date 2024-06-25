import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {MatButtonModule} from "@angular/material/button";

import {ErrorPageComponent} from "app/error/error-page.component";
import {ErrorRoutingModule} from "app/error/error-routing.module";
import {NotFoundPageComponent} from "app/error/not-found-page.component";

@NgModule({
              declarations: [
                  ErrorPageComponent,
                  NotFoundPageComponent
              ],
              imports     : [
                  ErrorRoutingModule,
                  CommonModule,
                  MatButtonModule
              ]
          })
export class ErrorModule {}
