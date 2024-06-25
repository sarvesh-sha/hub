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
export class JobSourcesApi
{
    constructor(private client: ApiClient, protected basePath: string)
    {
    }

    /**
    *
    *
    * @param id
    */
    public get__generateUrl(id: string): string
    {
        return this.basePath + '/job-sources/item/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async get(id: string): Promise<models.JobSource>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling get.');
        }

        const __path = this.get__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.JobSource> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.JobSource.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public getBatch__generateUrl(): string
    {
        return this.basePath + '/job-sources/batch';
    }

    /**
     *
     *
     * @param body
     */
    public async getBatch(body?: Array<string>): Promise<Array<models.JobSource>>
    {

        const __path = this.getBatch__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <Array<models.JobSource>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.JobSource.fixupPrototype(val);
            }
        }

        return __res;
    }

}
