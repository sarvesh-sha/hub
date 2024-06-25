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
export class RegistryTaggedImagesApi
{
    constructor(private client: ApiClient, protected basePath: string)
    {
    }

    /**
    *
    *
    * @param id
    */
    public distribute__generateUrl(id: string): string
    {
        return this.basePath + '/registry-tagged-images/item/${id}/distribute'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param status
     * @param hostId
     */
    public distribute(id: string, status?: string, hostId?: string): Promise<number>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling distribute.');
        }

        const __path = this.distribute__generateUrl(id);

        let __requestOptions = new ApiRequest();

        if (status !== undefined)
        {
            __requestOptions.setQueryParam('status', <any>status);
        }

        if (hostId !== undefined)
        {
            __requestOptions.setQueryParam('hostId', <any>hostId);
        }

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    * @param id
    */
    public get__generateUrl(id: string): string
    {
        return this.basePath + '/registry-tagged-images/item/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async get(id: string): Promise<models.RegistryTaggedImage>
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

        let __res = <models.RegistryTaggedImage> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.RegistryTaggedImage.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public getBatch__generateUrl(): string
    {
        return this.basePath + '/registry-tagged-images/batch';
    }

    /**
     *
     *
     * @param body
     */
    public async getBatch(body?: Array<string>): Promise<Array<models.RegistryTaggedImage>>
    {

        const __path = this.getBatch__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <Array<models.RegistryTaggedImage>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.RegistryTaggedImage.fixupPrototype(val);
            }
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public getUsage__generateUrl(id: string): string
    {
        return this.basePath + '/registry-tagged-images/item/${id}/usage'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async getUsage(id: string): Promise<models.RegistryTaggedImageUsage>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling getUsage.');
        }

        const __path = this.getUsage__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.RegistryTaggedImageUsage> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.RegistryTaggedImageUsage.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public lookupTagForRole__generateUrl(): string
    {
        return this.basePath + '/registry-tagged-images/lookup-tag';
    }

    /**
     *
     *
     * @param status
     * @param arch
     * @param role
     */
    public lookupTagForRole(status?: string, arch?: string, role?: string): Promise<string>
    {

        const __path = this.lookupTagForRole__generateUrl();

        let __requestOptions = new ApiRequest();

        if (status !== undefined)
        {
            __requestOptions.setQueryParam('status', <any>status);
        }

        if (arch !== undefined)
        {
            __requestOptions.setQueryParam('arch', <any>arch);
        }

        if (role !== undefined)
        {
            __requestOptions.setQueryParam('role', <any>role);
        }

        __requestOptions.setProduce('text/plain');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    * @param id
    * @param status
    */
    public mark__generateUrl(id: string, status: string): string
    {
        return this.basePath + '/registry-tagged-images/item/${id}/mark/${status}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)))
                   .replace('${' + 'status' + '}', encodeURIComponent(String(status)));
    }

    /**
     *
     *
     * @param id
     * @param status
     */
    public async mark(id: string, status: string): Promise<models.RegistryTaggedImage>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling mark.');
        }
        // verify required parameter 'status' is not null or undefined
        if (status === null || status === undefined)
        {
            throw new Error('Required parameter status was null or undefined when calling mark.');
        }

        const __path = this.mark__generateUrl(id, status);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "POST";

        let __res = <models.RegistryTaggedImage> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.RegistryTaggedImage.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public remove__generateUrl(id: string): string
    {
        return this.basePath + '/registry-tagged-images/item/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param dryRun
     */
    public async remove(id: string, dryRun?: boolean): Promise<models.ValidationResults>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling remove.');
        }

        const __path = this.remove__generateUrl(id);

        let __requestOptions = new ApiRequest();

        if (dryRun !== undefined)
        {
            __requestOptions.setQueryParam('dryRun', <any>dryRun);
        }

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "DELETE";

        let __res = <models.ValidationResults> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.ValidationResults.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param status
    */
    public report__generateUrl(status: string): string
    {
        return this.basePath + '/registry-tagged-images/report/${status}'
                   .replace('${' + 'status' + '}', encodeURIComponent(String(status)));
    }

    /**
     *
     *
     * @param status
     */
    public async report(status: string): Promise<Array<models.ReleaseStatusReport>>
    {
        // verify required parameter 'status' is not null or undefined
        if (status === null || status === undefined)
        {
            throw new Error('Required parameter status was null or undefined when calling report.');
        }

        const __path = this.report__generateUrl(status);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <Array<models.ReleaseStatusReport>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.ReleaseStatusReport.fixupPrototype(val);
            }
        }

        return __res;
    }

}
