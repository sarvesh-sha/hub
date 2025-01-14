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
export class UserPreferencesApi
{
    constructor(private client: ApiClient, protected basePath: string)
    {
    }

    /**
    *
    *
    * @param id
    */
    public checkValueFormat__generateUrl(id: string): string
    {
        return this.basePath + '/user-preferences/${id}/value/check'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param path
     * @param name
     * @param value
     */
    public checkValueFormat(id: string, path?: string, name?: string, value?: string): Promise<boolean>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling checkValueFormat.');
        }

        const __path = this.checkValueFormat__generateUrl(id);

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
    * @param id
    */
    public getValue__generateUrl(id: string): string
    {
        return this.basePath + '/user-preferences/${id}/value/get'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param path
     * @param name
     */
    public async getValue(id: string, path?: string, name?: string): Promise<models.UserPreference>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling getValue.');
        }

        const __path = this.getValue__generateUrl(id);

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

        let __res = <models.UserPreference> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.UserPreference.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public listSubKeys__generateUrl(id: string): string
    {
        return this.basePath + '/user-preferences/${id}/subkey/list'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param path
     */
    public listSubKeys(id: string, path?: string): Promise<Array<string>>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling listSubKeys.');
        }

        const __path = this.listSubKeys__generateUrl(id);

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
    * @param id
    */
    public listValues__generateUrl(id: string): string
    {
        return this.basePath + '/user-preferences/${id}/value/list'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param path
     */
    public listValues(id: string, path?: string): Promise<Array<string>>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling listValues.');
        }

        const __path = this.listValues__generateUrl(id);

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
    * @param id
    */
    public removeSubKeys__generateUrl(id: string): string
    {
        return this.basePath + '/user-preferences/${id}/subkey/remove'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param path
     */
    public removeSubKeys(id: string, path?: string): Promise<boolean>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling removeSubKeys.');
        }

        const __path = this.removeSubKeys__generateUrl(id);

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
    * @param id
    */
    public removeValue__generateUrl(id: string): string
    {
        return this.basePath + '/user-preferences/${id}/value/remove'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param path
     * @param name
     */
    public removeValue(id: string, path?: string, name?: string): Promise<boolean>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling removeValue.');
        }

        const __path = this.removeValue__generateUrl(id);

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
    * @param id
    */
    public setValue__generateUrl(id: string): string
    {
        return this.basePath + '/user-preferences/${id}/value/set'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param path
     * @param name
     * @param value
     */
    public setValue(id: string, path?: string, name?: string, value?: string): Promise<string>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling setValue.');
        }

        const __path = this.setValue__generateUrl(id);

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
