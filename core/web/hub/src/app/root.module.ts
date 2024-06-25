import {HttpClientModule} from "@angular/common/http";
import {ErrorHandler, NgModule} from "@angular/core";
import {MAT_TOOLTIP_DEFAULT_OPTIONS} from "@angular/material/tooltip";
import {BrowserModule} from "@angular/platform-browser";
import {BrowserAnimationsModule, NoopAnimationsModule} from "@angular/platform-browser/animations";
import {ServiceWorkerModule} from "@angular/service-worker";
import {AppNavigationResolver} from "app/app-navigation.resolver";
import {AppContext, AppEnvironmentConfiguration, AppErrorService, AppService} from "app/app.service";
import {RootRoutingModule} from "app/root-routing.module";
import {RootComponent} from "app/root.component";
import {AuthGuard} from "app/services/domain/auth.guard";
import {DomainModule} from "app/services/domain/domain.module";
import {UIModule} from "app/services/ui";
import {SwService} from "app/sw.service";
import {environment} from "environments/environment";
import {FrameworkServicesModule} from "framework/services";
import {ApiClientConfiguration} from "framework/services/api.client";

const isReport = window.location.href.indexOf("sys_asreport=true") >= 0;
const isTest   = window.location.href.indexOf("#/tests") >= 0;


// Due to AOT compilation an extra module is needed to control which animation module to load
@NgModule({
              exports: [BrowserModule]
          })
export class ConfigAnimationsModule
{
    static forRoot(isTest: boolean)
    {
        return {
            ngModule : ConfigAnimationsModule,
            providers: isTest
                ? (<any>NoopAnimationsModule).ɵinj.providers
                : (<any>BrowserAnimationsModule).ɵinj.providers
        };
    }
}

@NgModule({
              declarations: [
                  RootComponent
              ],
              imports     : [
                  // Core
                  ConfigAnimationsModule.forRoot(isTest),

                  // Services
                  HttpClientModule,
                  DomainModule,
                  FrameworkServicesModule,
                  UIModule,

                  // Routing
                  RootRoutingModule,

                  ServiceWorkerModule.register("ngsw-worker.js", {enabled: !isReport && environment.production})
              ],
              providers   : [
                  {
                      provide : ApiClientConfiguration,
                      useValue: {
                          apiDomain: environment.apiDomain,
                          apiPort  : environment.apiPort
                      }
                  },
                  {
                      provide : AppEnvironmentConfiguration,
                      useValue: {
                          version   : environment.version,
                          mapsApiKey: environment.mapsApiKey
                      }
                  },
                  {
                      provide : ErrorHandler,
                      useClass: AppErrorService
                  },
                  {
                      provide : MAT_TOOLTIP_DEFAULT_OPTIONS,
                      useValue: {disableTooltipInteractivity: true}
                  },
                  AppContext,
                  AppNavigationResolver,
                  AppService,
                  AuthGuard,
                  SwService
              ],
              exports     : [],
              bootstrap   : [RootComponent]
          })
export class RootModule {}
