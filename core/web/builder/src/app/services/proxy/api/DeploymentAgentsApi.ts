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
export class DeploymentAgentsApi
{
    constructor(private client: ApiClient, protected basePath: string)
    {
    }

    /**
    *
    *
    * @param id
    */
    public checkOnline__generateUrl(id: string): string
    {
        return this.basePath + '/deployment-agents/item/${id}/check-online'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public checkOnline(id: string): Promise<boolean>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling checkOnline.');
        }

        const __path = this.checkOnline__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    * @param id
    * @param session
    */
    public closeShell__generateUrl(id: string, session: string): string
    {
        return this.basePath + '/deployment-agents/item/${id}/shell/close/${session}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)))
                   .replace('${' + 'session' + '}', encodeURIComponent(String(session)));
    }

    /**
     *
     *
     * @param id
     * @param session
     */
    public closeShell(id: string, session: string): Promise<boolean>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling closeShell.');
        }
        // verify required parameter 'session' is not null or undefined
        if (session === null || session === undefined)
        {
            throw new Error('Required parameter session was null or undefined when calling closeShell.');
        }

        const __path = this.closeShell__generateUrl(id, session);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    * @param id
    */
    public configLogger__generateUrl(id: string): string
    {
        return this.basePath + '/deployment-agents/item/${id}/loggers/config'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param body
     */
    public async configLogger(id: string, body?: models.LoggerConfiguration): Promise<models.LoggerConfiguration>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling configLogger.');
        }

        const __path = this.configLogger__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <models.LoggerConfiguration> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.LoggerConfiguration.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public dumpThreads__generateUrl(id: string): string
    {
        return this.basePath + '/deployment-agents/item/${id}/threads'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param includeMemInfo
     */
    public dumpThreads(id: string, includeMemInfo?: boolean): Promise<string>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling dumpThreads.');
        }

        const __path = this.dumpThreads__generateUrl(id);

        let __requestOptions = new ApiRequest();

        if (includeMemInfo !== undefined)
        {
            __requestOptions.setQueryParam('includeMemInfo', <any>includeMemInfo);
        }

        __requestOptions.setProduce('text/plain');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    * @param id
    */
    public flush__generateUrl(id: string): string
    {
        return this.basePath + '/deployment-agents/item/${id}/flush'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public flush(id: string): Promise<boolean>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling flush.');
        }

        const __path = this.flush__generateUrl(id);

        let __requestOptions = new ApiRequest();

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
        return this.basePath + '/deployment-agents/item/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async get(id: string): Promise<models.DeploymentAgent>
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

        let __res = <models.DeploymentAgent> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DeploymentAgent.fixupPrototype(__res);
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
        return this.basePath + '/deployment-agents/all/${id}'
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
    public getAllShells__generateUrl(id: string): string
    {
        return this.basePath + '/deployment-agents/item/${id}/shell/all'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async getAllShells(id: string): Promise<Array<models.ShellToken>>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling getAllShells.');
        }

        const __path = this.getAllShells__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <Array<models.ShellToken>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.ShellToken.fixupPrototype(val);
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
        return this.basePath + '/deployment-agents/batch';
    }

    /**
     *
     *
     * @param body
     */
    public async getBatch(body?: Array<string>): Promise<Array<models.DeploymentAgent>>
    {

        const __path = this.getBatch__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <Array<models.DeploymentAgent>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.DeploymentAgent.fixupPrototype(val);
            }
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public getLoggers__generateUrl(id: string): string
    {
        return this.basePath + '/deployment-agents/item/${id}/loggers/list'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async getLoggers(id: string): Promise<Array<models.LoggerConfiguration>>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling getLoggers.');
        }

        const __path = this.getLoggers__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <Array<models.LoggerConfiguration>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.LoggerConfiguration.fixupPrototype(val);
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
        return this.basePath + '/deployment-agents/item/${id}/make-active'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public makeActive(id: string): Promise<boolean>
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

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    * @param id
    */
    public openShell__generateUrl(id: string): string
    {
        return this.basePath + '/deployment-agents/item/${id}/shell/new'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param cmd
     */
    public async openShell(id: string, cmd?: string): Promise<models.ShellToken>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling openShell.');
        }

        const __path = this.openShell__generateUrl(id);

        let __requestOptions = new ApiRequest();

        if (cmd !== undefined)
        {
            __requestOptions.setQueryParam('cmd', <any>cmd);
        }

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.ShellToken> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.ShellToken.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    * @param session
    */
    public readFromShell__generateUrl(id: string, session: string): string
    {
        return this.basePath + '/deployment-agents/item/${id}/shell/read/${session}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)))
                   .replace('${' + 'session' + '}', encodeURIComponent(String(session)));
    }

    /**
     *
     *
     * @param id
     * @param session
     */
    public async readFromShell(id: string, session: string): Promise<Array<models.ShellOutput>>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling readFromShell.');
        }
        // verify required parameter 'session' is not null or undefined
        if (session === null || session === undefined)
        {
            throw new Error('Required parameter session was null or undefined when calling readFromShell.');
        }

        const __path = this.readFromShell__generateUrl(id, session);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <Array<models.ShellOutput>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.ShellOutput.fixupPrototype(val);
            }
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
        return this.basePath + '/deployment-agents/item/${id}'
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
    public restart__generateUrl(id: string): string
    {
        return this.basePath + '/deployment-agents/item/${id}/restart'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public restart(id: string): Promise<boolean>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling restart.');
        }

        const __path = this.restart__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    * @param id
    */
    public terminate__generateUrl(id: string): string
    {
        return this.basePath + '/deployment-agents/item/${id}/terminate'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param dryRun
     */
    public async terminate(id: string, dryRun?: boolean): Promise<models.ValidationResults>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling terminate.');
        }

        const __path = this.terminate__generateUrl(id);

        let __requestOptions = new ApiRequest();

        if (dryRun !== undefined)
        {
            __requestOptions.setQueryParam('dryRun', <any>dryRun);
        }

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

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
    * @param session
    */
    public writeToShell__generateUrl(id: string, session: string): string
    {
        return this.basePath + '/deployment-agents/item/${id}/shell/write/${session}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)))
                   .replace('${' + 'session' + '}', encodeURIComponent(String(session)));
    }

    /**
     *
     *
     * @param id
     * @param session
     * @param body
     */
    public writeToShell(id: string, session: string, body?: models.ShellInput): Promise<boolean>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling writeToShell.');
        }
        // verify required parameter 'session' is not null or undefined
        if (session === null || session === undefined)
        {
            throw new Error('Required parameter session was null or undefined when calling writeToShell.');
        }

        const __path = this.writeToShell__generateUrl(id, session);

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

}
