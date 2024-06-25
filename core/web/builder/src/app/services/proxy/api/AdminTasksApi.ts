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
export class AdminTasksApi
{
    constructor(private client: ApiClient, protected basePath: string)
    {
    }

    /**
    *
    *
    */
    public checkUpgradeLevel__generateUrl(): string
    {
        return this.basePath + '/admin-tasks/upgrade/list';
    }

    /**
     *
     *
     */
    public checkUpgradeLevel(): Promise<Array<string>>
    {

        const __path = this.checkUpgradeLevel__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public checkUpgradeLevelForServices__generateUrl(): string
    {
        return this.basePath + '/admin-tasks/upgrade/list/services';
    }

    /**
     *
     *
     */
    public async checkUpgradeLevelForServices(): Promise<Array<models.CustomerUpgradeLevel>>
    {

        const __path = this.checkUpgradeLevelForServices__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <Array<models.CustomerUpgradeLevel>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.CustomerUpgradeLevel.fixupPrototype(val);
            }
        }

        return __res;
    }

    /**
    *
    *
    */
    public configLogger__generateUrl(): string
    {
        return this.basePath + '/admin-tasks/loggers/config';
    }

    /**
     *
     *
     * @param body
     */
    public async configLogger(body?: models.LoggerConfiguration): Promise<models.LoggerConfiguration>
    {

        const __path = this.configLogger__generateUrl();

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
    */
    public dumpDbConnections__generateUrl(): string
    {
        return this.basePath + '/admin-tasks/db-connections';
    }

    /**
     *
     *
     */
    public dumpDbConnections(): Promise<string>
    {

        const __path = this.dumpDbConnections__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('text/plain');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public dumpMessageBusStatistics__generateUrl(): string
    {
        return this.basePath + '/admin-tasks/message-bus-stats';
    }

    /**
     *
     *
     */
    public dumpMessageBusStatistics(): Promise<string>
    {

        const __path = this.dumpMessageBusStatistics__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('text/plain');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public dumpRequestStatistics__generateUrl(): string
    {
        return this.basePath + '/admin-tasks/request-stats';
    }

    /**
     *
     *
     */
    public dumpRequestStatistics(): Promise<string>
    {

        const __path = this.dumpRequestStatistics__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('text/plain');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public dumpRpcStatistics__generateUrl(): string
    {
        return this.basePath + '/admin-tasks/rpc-stats';
    }

    /**
     *
     *
     */
    public dumpRpcStatistics(): Promise<string>
    {

        const __path = this.dumpRpcStatistics__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('text/plain');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public dumpThreads__generateUrl(): string
    {
        return this.basePath + '/admin-tasks/threads';
    }

    /**
     *
     *
     */
    public dumpThreads(): Promise<string>
    {

        const __path = this.dumpThreads__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('text/plain');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public flushLogger__generateUrl(): string
    {
        return this.basePath + '/admin-tasks/logger/flush';
    }

    /**
     *
     *
     * @param toConsole
     */
    public flushLogger(toConsole?: boolean): Promise<string>
    {

        const __path = this.flushLogger__generateUrl();

        let __requestOptions = new ApiRequest();

        if (toConsole !== undefined)
        {
            __requestOptions.setQueryParam('toConsole', <any>toConsole);
        }

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public getAppVersion__generateUrl(): string
    {
        return this.basePath + '/admin-tasks/app-version';
    }

    /**
     *
     *
     */
    public getAppVersion(): Promise<string>
    {

        const __path = this.getAppVersion__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('text/plain');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public getDatagramSessions__generateUrl(): string
    {
        return this.basePath + '/admin-tasks/datagram-sessions';
    }

    /**
     *
     *
     */
    public async getDatagramSessions(): Promise<Array<models.MessageBusDatagramSession>>
    {

        const __path = this.getDatagramSessions__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <Array<models.MessageBusDatagramSession>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.MessageBusDatagramSession.fixupPrototype(val);
            }
        }

        return __res;
    }

    /**
    *
    *
    */
    public getLoggers__generateUrl(): string
    {
        return this.basePath + '/admin-tasks/loggers/list';
    }

    /**
     *
     *
     */
    public async getLoggers(): Promise<Array<models.LoggerConfiguration>>
    {

        const __path = this.getLoggers__generateUrl();

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
    */
    public pendingLogEntries__generateUrl(): string
    {
        return this.basePath + '/admin-tasks/logger/pending';
    }

    /**
     *
     *
     */
    public pendingLogEntries(): Promise<number>
    {

        const __path = this.pendingLogEntries__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

}
