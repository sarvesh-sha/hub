import {NgModule} from "@angular/core";

import {ApiClient, ApiClientConfiguration} from "framework/services/api.client";
import {CacheService} from "framework/services/cache.service";

@NgModule({
    providers: [
        ApiClientConfiguration,
        ApiClient,
        CacheService
    ],
})
export class FrameworkServicesModule {
}
