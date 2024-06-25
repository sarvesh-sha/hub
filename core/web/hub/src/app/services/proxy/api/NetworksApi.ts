/**
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Hub APIs
 * APIs and Definitions for the Optio3 Hub product.
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
    * @param networkId
    */
    public deleteLog__generateUrl(networkId: string): string
    {
        return this.basePath + '/networks/item/${networkId}/log'
                   .replace('${' + 'networkId' + '}', encodeURIComponent(String(networkId)));
    }

    /**
     *
     *
     * @param networkId
     * @param olderThanXMinutes
     */
    public deleteLog(networkId: string, olderThanXMinutes?: number): Promise<number>
    {
        // verify required parameter 'networkId' is not null or undefined
        if (networkId === null || networkId === undefined)
        {
            throw new Error('Required parameter networkId was null or undefined when calling deleteLog.');
        }

        const __path = this.deleteLog__generateUrl(networkId);

        let __requestOptions = new ApiRequest();

        if (olderThanXMinutes !== undefined)
        {
            __requestOptions.setQueryParam('olderThanXMinutes', <any>olderThanXMinutes);
        }

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "DELETE";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    * @param networkId
    */
    public filterLog__generateUrl(networkId: string): string
    {
        return this.basePath + '/networks/item/${networkId}/log/filter'
                   .replace('${' + 'networkId' + '}', encodeURIComponent(String(networkId)));
    }

    /**
     *
     *
     * @param networkId
     * @param body
     */
    public async filterLog(networkId: string, body?: models.LogEntryFilterRequest): Promise<Array<models.LogRange>>
    {
        // verify required parameter 'networkId' is not null or undefined
        if (networkId === null || networkId === undefined)
        {
            throw new Error('Required parameter networkId was null or undefined when calling filterLog.');
        }

        const __path = this.filterLog__generateUrl(networkId);

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <Array<models.LogRange>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.LogRange.fixupPrototype(val);
            }
        }

        return __res;
    }

    /**
    *
    *
    * @param networkId
    */
    public getLog__generateUrl(networkId: string): string
    {
        return this.basePath + '/networks/item/${networkId}/log'
                   .replace('${' + 'networkId' + '}', encodeURIComponent(String(networkId)));
    }

    /**
     *
     *
     * @param networkId
     * @param fromOffset
     * @param toOffset
     * @param limit
     */
    public async getLog(networkId: string, fromOffset?: number, toOffset?: number, limit?: number): Promise<Array<models.LogLine>>
    {
        // verify required parameter 'networkId' is not null or undefined
        if (networkId === null || networkId === undefined)
        {
            throw new Error('Required parameter networkId was null or undefined when calling getLog.');
        }

        const __path = this.getLog__generateUrl(networkId);

        let __requestOptions = new ApiRequest();

        if (fromOffset !== undefined)
        {
            __requestOptions.setQueryParam('fromOffset', <any>fromOffset);
        }

        if (toOffset !== undefined)
        {
            __requestOptions.setQueryParam('toOffset', <any>toOffset);
        }

        if (limit !== undefined)
        {
            __requestOptions.setQueryParam('limit', <any>limit);
        }

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <Array<models.LogLine>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.LogLine.fixupPrototype(val);
            }
        }

        return __res;
    }

    /**
    *
    *
    * @param networkId
    */
    public reclassify__generateUrl(networkId: string): string
    {
        return this.basePath + '/networks/item/${networkId}/reclassify'
                   .replace('${' + 'networkId' + '}', encodeURIComponent(String(networkId)));
    }

    /**
     *
     *
     * @param networkId
     */
    public reclassify(networkId: string): Promise<boolean>
    {
        // verify required parameter 'networkId' is not null or undefined
        if (networkId === null || networkId === undefined)
        {
            throw new Error('Required parameter networkId was null or undefined when calling reclassify.');
        }

        const __path = this.reclassify__generateUrl(networkId);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "PUT";

        return this.client.callWithOptions(__path, __requestOptions);
    }

}
