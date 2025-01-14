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
export class SystemPreferencesApi
{
    constructor(private client: ApiClient, protected basePath: string)
    {
    }

    /**
    *
    *
    */
    public checkValueFormat__generateUrl(): string
    {
        return this.basePath + '/system-preferences/value/check';
    }

    /**
     *
     *
     * @param path
     * @param name
     * @param value
     */
    public checkValueFormat(path?: string, name?: string, value?: string): Promise<boolean>
    {

        const __path = this.checkValueFormat__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/x-www-form-urlencoded');
        __requestOptions.setProduce('application/json');
        __requestOptions.hasFormParams = true;
        if (path !== undefined)
        {
            __requestOptions.setFormParam('path', <any>path);
        }
        if (name !== undefined)
        {
            __requestOptions.setFormParam('name', <any>name);
        }
        if (value !== undefined)
        {
            __requestOptions.setFormParam('value', <any>value);
        }
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public getValue__generateUrl(): string
    {
        return this.basePath + '/system-preferences/value/get';
    }

    /**
     *
     *
     * @param path
     * @param name
     */
    public async getValue(path?: string, name?: string): Promise<models.SystemPreference>
    {

        const __path = this.getValue__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/x-www-form-urlencoded');
        __requestOptions.setProduce('application/json');
        __requestOptions.hasFormParams = true;
        if (path !== undefined)
        {
            __requestOptions.setFormParam('path', <any>path);
        }
        if (name !== undefined)
        {
            __requestOptions.setFormParam('name', <any>name);
        }
        __requestOptions.method = "POST";

        let __res = <models.SystemPreference> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.SystemPreference.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public listSubKeys__generateUrl(): string
    {
        return this.basePath + '/system-preferences/subkey/list';
    }

    /**
     *
     *
     * @param path
     */
    public listSubKeys(path?: string): Promise<Array<string>>
    {

        const __path = this.listSubKeys__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/x-www-form-urlencoded');
        __requestOptions.setProduce('application/json');
        __requestOptions.hasFormParams = true;
        if (path !== undefined)
        {
            __requestOptions.setFormParam('path', <any>path);
        }
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public listValues__generateUrl(): string
    {
        return this.basePath + '/system-preferences/value/list';
    }

    /**
     *
     *
     * @param path
     */
    public listValues(path?: string): Promise<Array<string>>
    {

        const __path = this.listValues__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/x-www-form-urlencoded');
        __requestOptions.setProduce('application/json');
        __requestOptions.hasFormParams = true;
        if (path !== undefined)
        {
            __requestOptions.setFormParam('path', <any>path);
        }
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public removeSubKeys__generateUrl(): string
    {
        return this.basePath + '/system-preferences/subkey/remove';
    }

    /**
     *
     *
     * @param path
     */
    public removeSubKeys(path?: string): Promise<boolean>
    {

        const __path = this.removeSubKeys__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/x-www-form-urlencoded');
        __requestOptions.setProduce('text/plain');
        __requestOptions.hasFormParams = true;
        if (path !== undefined)
        {
            __requestOptions.setFormParam('path', <any>path);
        }
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public removeValue__generateUrl(): string
    {
        return this.basePath + '/system-preferences/value/remove';
    }

    /**
     *
     *
     * @param path
     * @param name
     */
    public removeValue(path?: string, name?: string): Promise<boolean>
    {

        const __path = this.removeValue__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/x-www-form-urlencoded');
        __requestOptions.setProduce('text/plain');
        __requestOptions.hasFormParams = true;
        if (path !== undefined)
        {
            __requestOptions.setFormParam('path', <any>path);
        }
        if (name !== undefined)
        {
            __requestOptions.setFormParam('name', <any>name);
        }
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public setValue__generateUrl(): string
    {
        return this.basePath + '/system-preferences/value/set';
    }

    /**
     *
     *
     * @param path
     * @param name
     * @param value
     */
    public setValue(path?: string, name?: string, value?: string): Promise<string>
    {

        const __path = this.setValue__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/x-www-form-urlencoded');
        __requestOptions.setProduce('text/plain');
        __requestOptions.hasFormParams = true;
        if (path !== undefined)
        {
            __requestOptions.setFormParam('path', <any>path);
        }
        if (name !== undefined)
        {
            __requestOptions.setFormParam('name', <any>name);
        }
        if (value !== undefined)
        {
            __requestOptions.setFormParam('value', <any>value);
        }
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

}
