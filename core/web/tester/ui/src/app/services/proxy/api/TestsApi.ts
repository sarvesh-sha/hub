/**
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Api documentation
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
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
export class TestsApi
{
    constructor(private client: ApiClient, protected basePath: string)
    {
    }

    /**
    *
    *
    * @param id
    */
    public cancelTest__generateUrl(id: string): string
    {
        return this.basePath + '/api/v1/tests/cancel/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public cancelTest(id: string): Promise<boolean>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling cancelTest.');
        }

        const __path = this.cancelTest__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public cancelTests__generateUrl(): string
    {
        return this.basePath + '/api/v1/tests/cancel';
    }

    /**
     *
     *
     */
    public cancelTests(): Promise<boolean>
    {

        const __path = this.cancelTests__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public checkProgress__generateUrl(): string
    {
        return this.basePath + '/api/v1/tests/progress';
    }

    /**
     *
     *
     */
    public async checkProgress(): Promise<Array<models.TestResult>>
    {

        const __path = this.checkProgress__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <Array<models.TestResult>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.TestResult.fixupPrototype(val);
            }
        }

        return __res;
    }

    /**
    *
    *
    */
    public getTests__generateUrl(): string
    {
        return this.basePath + '/api/v1/tests';
    }

    /**
     *
     *
     * @param body
     */
    public async getTests(body?: models.TestsInitializeRequest): Promise<Array<models.TestCase>>
    {

        const __path = this.getTests__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <Array<models.TestCase>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.TestCase.fixupPrototype(val);
            }
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public getVideo__generateUrl(id: string): string
    {
        return this.basePath + '/api/v1/tests/video/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public getVideo(id: string): Promise<void>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling getVideo.');
        }

        const __path = this.getVideo__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('video/mp4');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public runAll__generateUrl(): string
    {
        return this.basePath + '/api/v1/tests/run-all';
    }

    /**
     *
     *
     * @param body
     */
    public runAll(body?: models.TestsInitializeRequest): Promise<boolean>
    {

        const __path = this.runAll__generateUrl();

        let __requestOptions = new ApiRequest();

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
    public startTests__generateUrl(): string
    {
        return this.basePath + '/api/v1/tests/start';
    }

    /**
     *
     *
     * @param body
     */
    public async startTests(body?: models.TestsRunRequest): Promise<Array<models.TestCase>>
    {

        const __path = this.startTests__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <Array<models.TestCase>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.TestCase.fixupPrototype(val);
            }
        }

        return __res;
    }

}
