import {APP_BASE_HREF} from "@angular/common";
import {HttpClientModule} from "@angular/common/http";
import {NgModule} from "@angular/core";
import {FormsModule} from "@angular/forms";
import {BrowserModule} from "@angular/platform-browser";
import {RouterModule} from "@angular/router";
import {AppComponent} from "app/app.component";
import {environment} from "../environments/environment";

import {FrameworkServicesModule} from "../framework/services";
import {ApiClientConfiguration} from "../framework/services/api.client";
import {WebDataConnectorComponent} from "./wdc/web-data-connector.component";

@NgModule({
              declarations: [
                  AppComponent,
                  WebDataConnectorComponent
              ],
              imports     : [
                  BrowserModule,
                  FormsModule,
                  HttpClientModule,
                  FrameworkServicesModule,
                  RouterModule.forRoot([], {relativeLinkResolution: "legacy"})
              ],
              providers   : [
                  {
                      provide : ApiClientConfiguration,
                      useValue: {apiDomain: environment.apiDomain}
                  },
                  {
                      provide : APP_BASE_HREF,
                      useValue: environment.appBase
                  }
              ],
              bootstrap   : [AppComponent]
          })
export class AppModule
{
}
