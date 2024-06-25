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
export class RegistryImagesApi
{
    constructor(private client: ApiClient, protected basePath: string)
    {
    }

    /**
    *
    *
    * @param id
    */
    public checkRefresh__generateUrl(id: string): string
    {
        return this.basePath + '/registry-images/refresh/check/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param detailed
     */
    public async checkRefresh(id: string, detailed?: boolean): Promise<models.RegistryRefresh>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling checkRefresh.');
        }

        const __path = this.checkRefresh__generateUrl(id);

        let __requestOptions = new ApiRequest();

        if (detailed !== undefined)
        {
            __requestOptions.setQueryParam('detailed', <any>detailed);
        }

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.RegistryRefresh> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.RegistryRefresh.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param imageSha
    */
    public findBySha__generateUrl(imageSha: string): string
    {
        return this.basePath + '/registry-images/find/${imageSha}'
                   .replace('${' + 'imageSha' + '}', encodeURIComponent(String(imageSha)));
    }

    /**
     *
     *
     * @param imageSha
     */
    public async findBySha(imageSha: string): Promise<models.RegistryImage>
    {
        // verify required parameter 'imageSha' is not null or undefined
        if (imageSha === null || imageSha === undefined)
        {
            throw new Error('Required parameter imageSha was null or undefined when calling findBySha.');
        }

        const __path = this.findBySha__generateUrl(imageSha);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.RegistryImage> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.RegistryImage.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public get__generateUrl(id: string): string
    {
        return this.basePath + '/registry-images/item/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async get(id: string): Promise<models.RegistryImage>
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

        let __res = <models.RegistryImage> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.RegistryImage.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public getAll__generateUrl(): string
    {
        return this.basePath + '/registry-images/all';
    }

    /**
     *
     *
     */
    public async getAll(): Promise<Array<models.RecordIdentity>>
    {

        const __path = this.getAll__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <Array<models.RecordIdentity>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.RecordIdentity.fixupPrototype(val);
            }
        }

        return __res;
    }

    /**
    *
    *
    */
    public getBatch__generateUrl(): string
    {
        return this.basePath + '/registry-images/batch';
    }

    /**
     *
     *
     * @param body
     */
    public async getBatch(body?: Array<string>): Promise<Array<models.RegistryImage>>
    {

        const __path = this.getBatch__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <Array<models.RegistryImage>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.RegistryImage.fixupPrototype(val);
            }
        }

        return __res;
    }

    /**
    *
    *
    */
    public startRefresh__generateUrl(): string
    {
        return this.basePath + '/registry-images/refresh/start';
    }

    /**
     *
     *
     */
    public startRefresh(): Promise<string>
    {

        const __path = this.startRefresh__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('text/plain');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

}