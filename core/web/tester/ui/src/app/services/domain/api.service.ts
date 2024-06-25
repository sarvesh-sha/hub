import {Injectable} from "@angular/core";
import * as Apis from "app/services/proxy/api/api";

import {ApiClient} from "framework/services/api.client";

@Injectable({providedIn: "root"})
export class ApiService
{
    readonly basePath: string;

    tests: Apis.TestsApi;

    constructor(private client: ApiClient)
    {
        let origin   = client.configuration.apiDomain;
        let location = document.location;

        if (!origin || origin == "")
        {
            if (client.configuration.apiPort)
            {
                origin = `${location.protocol}//${location.hostname}:${client.configuration.apiPort}`;
            }
            else
            {
                origin = location.origin;
            }
        }

        this.basePath = origin;

        this.tests = new Apis.TestsApi(client, this.basePath);
    }
}
