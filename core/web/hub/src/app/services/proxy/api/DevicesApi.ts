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
export class DevicesApi
{
    constructor(private client: ApiClient, protected basePath: string)
    {
    }

    /**
    *
    *
    * @param id
    */
    public checkSummaryReport__generateUrl(id: string): string
    {
        return this.basePath + '/devices/summary/report/check/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param detailed
     */
    public async checkSummaryReport(id: string, detailed?: boolean): Promise<models.DevicesReportProgress>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling checkSummaryReport.');
        }

        const __path = this.checkSummaryReport__generateUrl(id);

        let __requestOptions = new ApiRequest();

        if (detailed !== undefined)
        {
            __requestOptions.setQueryParam('detailed', <any>detailed);
        }

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.DevicesReportProgress> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DevicesReportProgress.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public getDeviceHealth__generateUrl(id: string): string
    {
        return this.basePath + '/devices/health/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async getDeviceHealth(id: string): Promise<models.DeviceHealthSummary>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling getDeviceHealth.');
        }

        const __path = this.getDeviceHealth__generateUrl(id);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "POST";

        let __res = <models.DeviceHealthSummary> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DeviceHealthSummary.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public runRediscovery__generateUrl(id: string): string
    {
        return this.basePath + '/devices/rediscovery/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param forceListObjects
     * @param forceReadObjects
     */
    public async runRediscovery(id: string, forceListObjects?: boolean, forceReadObjects?: boolean): Promise<models.RecordIdentity>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling runRediscovery.');
        }

        const __path = this.runRediscovery__generateUrl(id);

        let __requestOptions = new ApiRequest();

        if (forceListObjects !== undefined)
        {
            __requestOptions.setQueryParam('forceListObjects', <any>forceListObjects);
        }

        if (forceReadObjects !== undefined)
        {
            __requestOptions.setQueryParam('forceReadObjects', <any>forceReadObjects);
        }

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "POST";

        let __res = <models.RecordIdentity> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.RecordIdentity.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public startSummaryReport__generateUrl(): string
    {
        return this.basePath + '/devices/summary/report';
    }

    /**
     *
     *
     */
    public startSummaryReport(): Promise<string>
    {

        const __path = this.startSummaryReport__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('text/plain');
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    * @param id
    * @param fileName
    */
    public streamSummaryReport__generateUrl(id: string, fileName: string): string
    {
        return this.basePath + '/devices/summary/report/excel/${id}/${fileName}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)))
                   .replace('${' + 'fileName' + '}', encodeURIComponent(String(fileName)));
    }

    /**
     *
     *
     * @param id
     * @param fileName
     */
    public streamSummaryReport(id: string, fileName: string): Promise<Blob>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling streamSummaryReport.');
        }
        // verify required parameter 'fileName' is not null or undefined
        if (fileName === null || fileName === undefined)
        {
            throw new Error('Required parameter fileName was null or undefined when calling streamSummaryReport.');
        }

        const __path = this.streamSummaryReport__generateUrl(id, fileName);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/octet-stream');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

}