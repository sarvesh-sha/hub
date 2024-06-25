import {HttpClientModule} from "@angular/common/http";
import {ErrorHandler, NgModule} from "@angular/core";
import {FormsModule} from "@angular/forms";
// 3rd part dependencies
import {BrowserModule} from "@angular/platform-browser";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {NoPreloading, RouterModule} from "@angular/router";
import {AppNavigationResolver} from "app/app-navigation.resolver";
// application
import {AppComponent} from "app/app.component";
import {ROUTES} from "app/app.routes";
import {AppContext, AppEnvironmentConfiguration, AppErrorService, AppService} from "app/app.service";
import {CustomerModule} from "app/customer";
import {DashboardModule} from "app/dashboard";
// ui component modules
import {ErrorModule} from "app/error";
import {ReportingLayoutComponent} from "app/layouts/reporting-layout.component";
// layouts
import {StandardLayoutComponent, StandardLayoutMenuDirective} from "app/layouts/standard-layout.component";
//import {ReportsModule} from "app/reports";
// service layer
import {DomainModule} from "app/services/domain";
import {SharedModule} from "app/shared";
import {StartModule} from "app/start";
// configuration
import {environment} from "environments/environment";
import {CdkModule} from "framework/cdk";
// framework
import {MaterialModule} from "framework/material";
import {FrameworkServicesModule} from "framework/services";
import {ApiClientConfiguration} from "framework/services/api.client";
import {FrameworkUIModule} from "framework/ui";
import {ErrorListPaneModule} from "framework/ui/errors/error-list-pane.module";

/**
 * Root Module
 *
 * App bootstrap here, add your component (Page) to var [declarations] for load.
 */
@NgModule({
              declarations: [
                  // Page
                  AppComponent,
                  StandardLayoutComponent,
                  StandardLayoutMenuDirective,
                  ReportingLayoutComponent
              ],
              imports     : [
                  // Angular Imports
                  BrowserModule,
                  FormsModule,
                  HttpClientModule,

                  // framework
                  MaterialModule,
                  CdkModule,
                  FrameworkServicesModule,
                  FrameworkUIModule,

                  // core/domain services
                  DomainModule,

                  // ui component modules
                  ErrorModule,
                  SharedModule,
                  StartModule,
                  DashboardModule,
                  CustomerModule,
                  //ReportsModule,

                  // Extra Plugin Imports
                  // If you using lazy loading, var [preloadingStrategy] can change to PreloadAllModules or NoPreloading.
                  RouterModule.forRoot(ROUTES, {
                      useHash           : true,
                      preloadingStrategy: NoPreloading
                  }),
                  BrowserAnimationsModule,
                  ErrorListPaneModule
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
                  AppNavigationResolver,
                  AppService,
                  AppContext
              ],
              bootstrap   : [AppComponent]
          })
export class AppModule
{

}
