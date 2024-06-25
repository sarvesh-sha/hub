import {OverlayModule} from "@angular/cdk/overlay";
import {NgModule} from "@angular/core";

import {ApiClient, ApiClientConfiguration} from "framework/services/api.client";
import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {LoggingService} from "framework/services/logging.service";
import {UtilsService} from "framework/services/utils.service";
import {ComponentContext, ComponentCoreContext, ComponentFrameworkContext, ComponentRoutingContext, ComponentUIFrameworkContext} from "framework/ui/components";
import {AppFabsService} from "framework/ui/fabs/app-fabs.service";
import {AppNavigationService} from "framework/ui/navigation/app-navigation.service";
import {ViewStateService} from "framework/ui/navigation/view-state.service";
import {PrintingService} from "framework/ui/reporting/printing.service";
import {PuppeteerService} from "framework/ui/reporting/puppeteer.service";

@NgModule({
              imports  : [
                  OverlayModule
              ],
              providers: [
                  UtilsService,
                  LoggingService,
                  CacheService,
                  ApiClientConfiguration,
                  ApiClient,
                  ErrorService,

                  AppFabsService,
                  AppNavigationService,
                  ViewStateService,
                  ComponentCoreContext,
                  ComponentRoutingContext,
                  ComponentFrameworkContext,
                  ComponentUIFrameworkContext,
                  ComponentContext,
                  PuppeteerService,
                  PrintingService
              ]
          })
export class FrameworkServicesModule {}
