// import dependencies
import {Injectable, Injector} from "@angular/core";

import * as Apis from "app/services/proxy/api/api";
import {ApiClient} from "framework/services/api.client";

@Injectable({providedIn: "root"})
export class ApiService
{
    assets: Apis.AssetsApi;
    assetTimeSeries: Apis.AssetTimeSeriesApi;
    dataConnection: Apis.DataConnectionApi;
    normalization: Apis.NormalizationsApi;
    userPreferences: Apis.UserPreferencesApi;
    users: Apis.UsersApi;

    constructor(private client: ApiClient,
                public injector: Injector)
    {
        let basePath = client.configuration.apiDomain + "/api/v1";

        this.assets                   = new Apis.AssetsApi(client, basePath);
        this.assetTimeSeries          = new Apis.AssetTimeSeriesApi(client, basePath);
        this.dataConnection           = new Apis.DataConnectionApi(client, basePath);
        this.normalization            = new Apis.NormalizationsApi(client, basePath);
        this.userPreferences          = new Apis.UserPreferencesApi(client, basePath);
        this.users                    = new Apis.UsersApi(client, basePath);
    }
}
