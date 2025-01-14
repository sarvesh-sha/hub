/**
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Builder APIs
 * APIs and Definitions for the Optio3 Builder product.
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
export class CustomerCommunicationsApi
{
    constructor(private client: ApiClient, protected basePath: string)
    {
    }

    /**
    *
    *
    */
    public registerDevice__generateUrl(): string
    {
        return this.basePath + '/customer-communications/register-device';
    }

    /**
     *
     *
     * @param customerId
     * @param customerAccessKey
     * @param body
     */
    public registerDevice(customerId?: string, customerAccessKey?: string, body?: models.DeviceDetails): Promise<string>
    {

        const __path = this.registerDevice__generateUrl();

        let __requestOptions = new ApiRequest();

        if (customerId !== undefined)
        {
            __requestOptions.setQueryParam('customerId', <any>customerId);
        }

        if (customerAccessKey !== undefined)
        {
            __requestOptions.setQueryParam('customerAccessKey', <any>customerAccessKey);
        }

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public reportCrash__generateUrl(): string
    {
        return this.basePath + '/customer-communications/report-crash';
    }

    /**
     *
     *
     * @param customerId
     * @param customerAccessKey
     * @param body
     */
    public reportCrash(customerId?: string, customerAccessKey?: string, body?: models.CrashReport): Promise<string>
    {

        const __path = this.reportCrash__generateUrl();

        let __requestOptions = new ApiRequest();

        if (customerId !== undefined)
        {
            __requestOptions.setQueryParam('customerId', <any>customerId);
        }

        if (customerAccessKey !== undefined)
        {
            __requestOptions.setQueryParam('customerAccessKey', <any>customerAccessKey);
        }

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public sendEmail__generateUrl(): string
    {
        return this.basePath + '/customer-communications/send-email';
    }

    /**
     *
     *
     * @param customerId
     * @param customerAccessKey
     * @param body
     */
    public sendEmail(customerId?: string, customerAccessKey?: string, body?: models.EmailMessage): Promise<string>
    {

        const __path = this.sendEmail__generateUrl();

        let __requestOptions = new ApiRequest();

        if (customerId !== undefined)
        {
            __requestOptions.setQueryParam('customerId', <any>customerId);
        }

        if (customerAccessKey !== undefined)
        {
            __requestOptions.setQueryParam('customerAccessKey', <any>customerAccessKey);
        }

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public sendText__generateUrl(): string
    {
        return this.basePath + '/customer-communications/send-text';
    }

    /**
     *
     *
     * @param customerId
     * @param customerAccessKey
     * @param body
     */
    public sendText(customerId?: string, customerAccessKey?: string, body?: models.TextMessage): Promise<string>
    {

        const __path = this.sendText__generateUrl();

        let __requestOptions = new ApiRequest();

        if (customerId !== undefined)
        {
            __requestOptions.setQueryParam('customerId', <any>customerId);
        }

        if (customerAccessKey !== undefined)
        {
            __requestOptions.setQueryParam('customerAccessKey', <any>customerAccessKey);
        }

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

}
