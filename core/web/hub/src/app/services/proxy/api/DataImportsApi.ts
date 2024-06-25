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
export class DataImportsApi
{
    constructor(private client: ApiClient, protected basePath: string)
    {
    }

    /**
    *
    *
    * @param id
    */
    public checkImport__generateUrl(id: string): string
    {
        return this.basePath + '/data-imports/check/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param detailed
     */
    public async checkImport(id: string, detailed?: boolean): Promise<models.DataImportProgress>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling checkImport.');
        }

        const __path = this.checkImport__generateUrl(id);

        let __requestOptions = new ApiRequest();

        if (detailed !== undefined)
        {
            __requestOptions.setQueryParam('detailed', <any>detailed);
        }

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.DataImportProgress> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DataImportProgress.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public create__generateUrl(): string
    {
        return this.basePath + '/data-imports/create';
    }

    /**
     *
     *
     * @param body
     */
    public async create(body?: models.ImportedMetadata): Promise<models.ImportedMetadata>
    {

        const __path = this.create__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <models.ImportedMetadata> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.ImportedMetadata.fixupPrototype(__res);
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
        return this.basePath + '/data-imports/item/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param detailed
     */
    public async get(id: string, detailed?: boolean): Promise<models.ImportedMetadata>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling get.');
        }

        const __path = this.get__generateUrl(id);

        let __requestOptions = new ApiRequest();

        if (detailed !== undefined)
        {
            __requestOptions.setQueryParam('detailed', <any>detailed);
        }

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.ImportedMetadata> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.ImportedMetadata.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public getAll__generateUrl(): string
    {
        return this.basePath + '/data-imports/all';
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
        return this.basePath + '/data-imports/batch';
    }

    /**
     *
     *
     * @param body
     */
    public async getBatch(body?: Array<string>): Promise<Array<models.ImportedMetadata>>
    {

        const __path = this.getBatch__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <Array<models.ImportedMetadata>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.ImportedMetadata.fixupPrototype(val);
            }
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public makeActive__generateUrl(id: string): string
    {
        return this.basePath + '/data-imports/item/${id}/activate'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async makeActive(id: string): Promise<models.ImportedMetadata>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling makeActive.');
        }

        const __path = this.makeActive__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.ImportedMetadata> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.ImportedMetadata.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public parseImport__generateUrl(): string
    {
        return this.basePath + '/data-imports/parse-import';
    }

    /**
     *
     *
     * @param body
     */
    public async parseImport(body?: models.RawImport): Promise<models.ImportedMetadata>
    {

        const __path = this.parseImport__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <models.ImportedMetadata> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.ImportedMetadata.fixupPrototype(__res);
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
        return this.basePath + '/data-imports/item/${id}'
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
    */
    public startImport__generateUrl(): string
    {
        return this.basePath + '/data-imports/start';
    }

    /**
     *
     *
     * @param body
     */
    public startImport(body?: models.DataImportRun): Promise<string>
    {

        const __path = this.startImport__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('text/plain');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

}
