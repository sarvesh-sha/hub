// import dependencies
import {Injectable, Injector} from "@angular/core";

import * as Apis from "app/services/proxy/api/api";

import {ApiClient} from "framework/services/api.client";

@Injectable()
export class ApiService
{
    readonly basePath: string;

    adminTasks: Apis.AdminTasksApi;
    networks: Apis.NetworksApi;
    provision: Apis.ProvisionApi;

    constructor(private client: ApiClient,
                public injector: Injector)
    {
        let origin   = this.client.configuration.apiDomain;
        let location = document.location;

        if (!origin || origin == "")
        {
            if (this.client.configuration.apiPort)
            {
                origin = `${location.protocol}//${location.hostname}:${this.client.configuration.apiPort}`;
            }
            else
            {
                origin = location.origin;
            }
        }

        this.basePath = origin + "/api/v1";

        this.adminTasks = new Apis.AdminTasksApi(client, this.basePath);
        this.networks   = new Apis.NetworksApi(client, this.basePath);
        this.provision  = new Apis.ProvisionApi(client, this.basePath);
    }
}
