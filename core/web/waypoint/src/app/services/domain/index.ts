import {Injectable, NgModule} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";
import {ApiClientConfiguration} from "framework/services/api.client";

@Injectable()
export class AppDomainContext {
    constructor(public apis: ApiService) {
    }
}

@NgModule({
    providers: [
        ApiService,
        ApiClientConfiguration,
        AppDomainContext
    ]
})
export class DomainModule {
}
