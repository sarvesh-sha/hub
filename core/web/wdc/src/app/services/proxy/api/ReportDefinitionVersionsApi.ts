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
export class ReportDefinitionVersionsApi
{
    constructor(private client: ApiClient, protected basePath: string)
    {
    }

    /**
    *
    *
    */
    public create__generateUrl(): string
    {
        return this.basePath + '/report-definition-versions/create';
    }

    /**
     *
     *
     * @param body
     */
    public async create(body?: models.ReportDefinitionVersion): Promise<models.ReportDefinitionVersion>
    {

        const __path = this.create__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <models.ReportDefinitionVersion> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.ReportDefinitionVersion.fixupPrototype(__res);
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
        return this.basePath + '/report-definition-versions/item/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async get(id: string): Promise<models.ReportDefinitionVersion>
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

        let __res = <models.ReportDefinitionVersion> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.ReportDefinitionVersion.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public getBatch__generateUrl(): string
    {
        return this.basePath + '/report-definition-versions/batch';
    }

    /**
     *
     *
     * @param body
     */
    public async getBatch(body?: Array<string>): Promise<Array<models.ReportDefinitionVersion>>
    {

        const __path = this.getBatch__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <Array<models.ReportDefinitionVersion>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.ReportDefinitionVersion.fixupPrototype(val);
            }
        }

        return __res;
    }

    /**
    *
    *
    * @param predId
    * @param succId
    */
    public link__generateUrl(predId: string, succId: string): string
    {
        return this.basePath + '/report-definition-versions/item/${predId}/link/${succId}'
                   .replace('${' + 'predId' + '}', encodeURIComponent(String(predId)))
                   .replace('${' + 'succId' + '}', encodeURIComponent(String(succId)));
    }

    /**
     *
     *
     * @param predId
     * @param succId
     */
    public async link(predId: string, succId: string): Promise<models.ReportDefinitionVersion>
    {
        // verify required parameter 'predId' is not null or undefined
        if (predId === null || predId === undefined)
        {
            throw new Error('Required parameter predId was null or undefined when calling link.');
        }
        // verify required parameter 'succId' is not null or undefined
        if (succId === null || succId === undefined)
        {
            throw new Error('Required parameter succId was null or undefined when calling link.');
        }

        const __path = this.link__generateUrl(predId, succId);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.ReportDefinitionVersion> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.ReportDefinitionVersion.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public makeHead__generateUrl(id: string): string
    {
        return this.basePath + '/report-definition-versions/item/${id}/make-head'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async makeHead(id: string): Promise<models.ReportDefinitionVersion>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling makeHead.');
        }

        const __path = this.makeHead__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.ReportDefinitionVersion> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.ReportDefinitionVersion.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public makeRelease__generateUrl(id: string): string
    {
        return this.basePath + '/report-definition-versions/item/${id}/make-release'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async makeRelease(id: string): Promise<models.ReportDefinitionVersion>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling makeRelease.');
        }

        const __path = this.makeRelease__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.ReportDefinitionVersion> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.ReportDefinitionVersion.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public parseImport__generateUrl(): string
    {
        return this.basePath + '/report-definition-versions/parse-import';
    }

    /**
     *
     *
     * @param body
     */
    public async parseImport(body?: models.RawImport): Promise<models.ReportDefinitionVersion>
    {

        const __path = this.parseImport__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <models.ReportDefinitionVersion> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.ReportDefinitionVersion.fixupPrototype(__res);
        }

        return __res;
    }

}
