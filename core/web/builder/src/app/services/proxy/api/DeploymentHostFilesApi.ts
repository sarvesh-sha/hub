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
export class DeploymentHostFilesApi
{
    constructor(private client: ApiClient, protected basePath: string)
    {
    }

    /**
    *
    *
    * @param hostId
    */
    public create__generateUrl(hostId: string): string
    {
        return this.basePath + '/deployment-host-files/create/${hostId}'
                   .replace('${' + 'hostId' + '}', encodeURIComponent(String(hostId)));
    }

    /**
     *
     *
     * @param hostId
     * @param body
     */
    public async create(hostId: string, body?: models.DeploymentHostFile): Promise<models.DeploymentHostFile>
    {
        // verify required parameter 'hostId' is not null or undefined
        if (hostId === null || hostId === undefined)
        {
            throw new Error('Required parameter hostId was null or undefined when calling create.');
        }

        const __path = this.create__generateUrl(hostId);

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <models.DeploymentHostFile> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DeploymentHostFile.fixupPrototype(__res);
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
        return this.basePath + '/deployment-host-files/item/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async get(id: string): Promise<models.DeploymentHostFile>
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

        let __res = <models.DeploymentHostFile> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DeploymentHostFile.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public getAll__generateUrl(id: string): string
    {
        return this.basePath + '/deployment-host-files/all/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async getAll(id: string): Promise<Array<models.RecordIdentity>>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling getAll.');
        }

        const __path = this.getAll__generateUrl(id);

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
    * @param id
    */
    public getAsBinary__generateUrl(id: string): string
    {
        return this.basePath + '/deployment-host-files/item/${id}/contents-get-as-binary'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async getAsBinary(id: string): Promise<models.DeploymentHostFileContents>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling getAsBinary.');
        }

        const __path = this.getAsBinary__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.DeploymentHostFileContents> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DeploymentHostFileContents.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public getAsText__generateUrl(id: string): string
    {
        return this.basePath + '/deployment-host-files/item/${id}/contents-get-as-text'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async getAsText(id: string): Promise<models.DeploymentHostFileContents>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling getAsText.');
        }

        const __path = this.getAsText__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.DeploymentHostFileContents> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DeploymentHostFileContents.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public getBatch__generateUrl(): string
    {
        return this.basePath + '/deployment-host-files/batch';
    }

    /**
     *
     *
     * @param body
     */
    public async getBatch(body?: Array<string>): Promise<Array<models.DeploymentHostFile>>
    {

        const __path = this.getBatch__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <Array<models.DeploymentHostFile>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.DeploymentHostFile.fixupPrototype(val);
            }
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    * @param fileName
    */
    public getStream__generateUrl(id: string, fileName: string): string
    {
        return this.basePath + '/deployment-host-files/item/${id}/stream/${fileName}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)))
                   .replace('${' + 'fileName' + '}', encodeURIComponent(String(fileName)));
    }

    /**
     *
     *
     * @param id
     * @param fileName
     */
    public getStream(id: string, fileName: string): Promise<any>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling getStream.');
        }
        // verify required parameter 'fileName' is not null or undefined
        if (fileName === null || fileName === undefined)
        {
            throw new Error('Required parameter fileName was null or undefined when calling getStream.');
        }

        const __path = this.getStream__generateUrl(id, fileName);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/octet-stream');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    * @param id
    */
    public remove__generateUrl(id: string): string
    {
        return this.basePath + '/deployment-host-files/item/${id}'
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
    * @param id
    */
    public setContents__generateUrl(id: string): string
    {
        return this.basePath + '/deployment-host-files/item/${id}/contents-set'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param body
     */
    public async setContents(id: string, body?: models.DeploymentHostFileContents): Promise<models.DeploymentHostFile>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling setContents.');
        }

        const __path = this.setContents__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <models.DeploymentHostFile> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DeploymentHostFile.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public setStream__generateUrl(id: string): string
    {
        return this.basePath + '/deployment-host-files/item/${id}/stream'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param body
     */
    public async setStream(id: string, body?: Blob): Promise<models.DeploymentHostFile>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling setStream.');
        }

        const __path = this.setStream__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/octet-stream');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <models.DeploymentHostFile> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DeploymentHostFile.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public startDownload__generateUrl(id: string): string
    {
        return this.basePath + '/deployment-host-files/item/${id}/start-download'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async startDownload(id: string): Promise<models.DeploymentHostFile>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling startDownload.');
        }

        const __path = this.startDownload__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.DeploymentHostFile> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DeploymentHostFile.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public startUpload__generateUrl(id: string): string
    {
        return this.basePath + '/deployment-host-files/item/${id}/start-upload'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async startUpload(id: string): Promise<models.DeploymentHostFile>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling startUpload.');
        }

        const __path = this.startUpload__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.DeploymentHostFile> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DeploymentHostFile.fixupPrototype(__res);
        }

        return __res;
    }

}