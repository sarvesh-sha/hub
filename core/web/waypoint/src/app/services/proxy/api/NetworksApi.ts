/**
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Waypoint APIs
 * APIs and Definitions for the Optio3 Waypoint product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

import { Inject, Injectable, Optional }  from '@angular/core';

import * as models               from '../model/models';
import { COLLECTION_FORMATS    } from '../variables';
import { ApiClient, ApiRequest } from 'framework/services/api.client';

/* tslint:disable:no-unused-variable member-ordering */


@Injectable()
export class NetworksApi
{
    constructor(private client: ApiClient, protected basePath: string)
    {
    }

    /**
    *
    *
    */
    public checkDestination__generateUrl(): string
    {
        return this.basePath + '/networks/check-destination';
    }

    /**
     *
     *
     * @param body
     */
    public async checkDestination(body?: models.NetworkDestinationRequest): Promise<models.NetworkDestinationResponse>
    {

        const __path = this.checkDestination__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <models.NetworkDestinationResponse> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.NetworkDestinationResponse.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public checkStatus__generateUrl(): string
    {
        return this.basePath + '/networks/check-status';
    }

    /**
     *
     *
     */
    public async checkStatus(): Promise<models.NetworkStatus>
    {

        const __path = this.checkStatus__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.NetworkStatus> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.NetworkStatus.fixupPrototype(__res);
        }

        return __res;
    }

}
