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
export class DiscoveryApi
{
    constructor(private client: ApiClient, protected basePath: string)
    {
    }

    /**
    *
    *
    * @param networkId
    */
    public autoConfig__generateUrl(networkId: string): string
    {
        return this.basePath + '/discovery/auto-config/${networkId}'
                   .replace('${' + 'networkId' + '}', encodeURIComponent(String(networkId)));
    }

    /**
     *
     *
     * @param networkId
     */
    public autoConfig(networkId: string): Promise<boolean>
    {
        // verify required parameter 'networkId' is not null or undefined
        if (networkId === null || networkId === undefined)
        {
            throw new Error('Required parameter networkId was null or undefined when calling autoConfig.');
        }

        const __path = this.autoConfig__generateUrl(networkId);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    * @param gatewayId
    * @param networkId
    */
    public bind__generateUrl(gatewayId: string, networkId: string): string
    {
        return this.basePath + '/discovery/bindings/${gatewayId}/add/${networkId}'
                   .replace('${' + 'gatewayId' + '}', encodeURIComponent(String(gatewayId)))
                   .replace('${' + 'networkId' + '}', encodeURIComponent(String(networkId)));
    }

    /**
     *
     *
     * @param gatewayId
     * @param networkId
     * @param forceDiscovery
     * @param forceListObjects
     * @param forceReadObjects
     */
    public bind(gatewayId: string, networkId: string, forceDiscovery?: boolean, forceListObjects?: boolean, forceReadObjects?: boolean): Promise<boolean>
    {
        // verify required parameter 'gatewayId' is not null or undefined
        if (gatewayId === null || gatewayId === undefined)
        {
            throw new Error('Required parameter gatewayId was null or undefined when calling bind.');
        }
        // verify required parameter 'networkId' is not null or undefined
        if (networkId === null || networkId === undefined)
        {
            throw new Error('Required parameter networkId was null or undefined when calling bind.');
        }

        const __path = this.bind__generateUrl(gatewayId, networkId);

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/x-www-form-urlencoded');
        __requestOptions.setProduce('application/json');
        __requestOptions.hasFormParams = true;
        if (forceDiscovery !== undefined)
        {
            __requestOptions.setFormParam('forceDiscovery', <any>forceDiscovery);
        }
        if (forceListObjects !== undefined)
        {
            __requestOptions.setFormParam('forceListObjects', <any>forceListObjects);
        }
        if (forceReadObjects !== undefined)
        {
            __requestOptions.setFormParam('forceReadObjects', <any>forceReadObjects);
        }
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    * @param id
    */
    public checkPushToAzureDigitalTwin__generateUrl(id: string): string
    {
        return this.basePath + '/discovery/azure-digital-twin/publish-check/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param detailed
     */
    public async checkPushToAzureDigitalTwin(id: string, detailed?: boolean): Promise<models.AzureDigitalTwinSyncProgress>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling checkPushToAzureDigitalTwin.');
        }

        const __path = this.checkPushToAzureDigitalTwin__generateUrl(id);

        let __requestOptions = new ApiRequest();

        if (detailed !== undefined)
        {
            __requestOptions.setQueryParam('detailed', <any>detailed);
        }

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.AzureDigitalTwinSyncProgress> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.AzureDigitalTwinSyncProgress.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public checkReport__generateUrl(id: string): string
    {
        return this.basePath + '/discovery/report/check/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param detailed
     */
    public async checkReport(id: string, detailed?: boolean): Promise<models.DiscoveryReportProgress>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling checkReport.');
        }

        const __path = this.checkReport__generateUrl(id);

        let __requestOptions = new ApiRequest();

        if (detailed !== undefined)
        {
            __requestOptions.setQueryParam('detailed', <any>detailed);
        }

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.DiscoveryReportProgress> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DiscoveryReportProgress.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public describeDeviceTemplates__generateUrl(): string
    {
        return this.basePath + '/discovery/device-templates/describe';
    }

    /**
     *
     *
     */
    public async describeDeviceTemplates(): Promise<models.DevicesTemplate>
    {

        const __path = this.describeDeviceTemplates__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.DevicesTemplate> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DevicesTemplate.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param gatewayId
    */
    public flushEntities__generateUrl(gatewayId: string): string
    {
        return this.basePath + '/discovery/bindings/${gatewayId}/flush'
                   .replace('${' + 'gatewayId' + '}', encodeURIComponent(String(gatewayId)));
    }

    /**
     *
     *
     * @param gatewayId
     */
    public async flushEntities(gatewayId: string): Promise<models.RecordIdentity>
    {
        // verify required parameter 'gatewayId' is not null or undefined
        if (gatewayId === null || gatewayId === undefined)
        {
            throw new Error('Required parameter gatewayId was null or undefined when calling flushEntities.');
        }

        const __path = this.flushEntities__generateUrl(gatewayId);

        let __requestOptions = new ApiRequest();

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
    * @param gatewayId
    */
    public flushHeartbeat__generateUrl(gatewayId: string): string
    {
        return this.basePath + '/discovery/bindings/${gatewayId}/flush-hb'
                   .replace('${' + 'gatewayId' + '}', encodeURIComponent(String(gatewayId)));
    }

    /**
     *
     *
     * @param gatewayId
     */
    public async flushHeartbeat(gatewayId: string): Promise<models.RecordIdentity>
    {
        // verify required parameter 'gatewayId' is not null or undefined
        if (gatewayId === null || gatewayId === undefined)
        {
            throw new Error('Required parameter gatewayId was null or undefined when calling flushHeartbeat.');
        }

        const __path = this.flushHeartbeat__generateUrl(gatewayId);

        let __requestOptions = new ApiRequest();

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
    public getAzureDigitalTwinCredentials__generateUrl(): string
    {
        return this.basePath + '/discovery/azure-digital-twin/cred';
    }

    /**
     *
     *
     */
    public async getAzureDigitalTwinCredentials(): Promise<models.AzureDigitalTwinsHelperCredentials>
    {

        const __path = this.getAzureDigitalTwinCredentials__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.AzureDigitalTwinsHelperCredentials> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.AzureDigitalTwinsHelperCredentials.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public getAzureIoTHubCredentials__generateUrl(): string
    {
        return this.basePath + '/discovery/azure-iot-hub/cred';
    }

    /**
     *
     *
     */
    public async getAzureIoTHubCredentials(): Promise<models.AzureIotHubHelperCredentials>
    {

        const __path = this.getAzureIoTHubCredentials__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.AzureIotHubHelperCredentials> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.AzureIotHubHelperCredentials.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param gatewayId
    */
    public getBindings__generateUrl(gatewayId: string): string
    {
        return this.basePath + '/discovery/bindings/${gatewayId}'
                   .replace('${' + 'gatewayId' + '}', encodeURIComponent(String(gatewayId)));
    }

    /**
     *
     *
     * @param gatewayId
     */
    public async getBindings(gatewayId: string): Promise<Array<models.RecordIdentity>>
    {
        // verify required parameter 'gatewayId' is not null or undefined
        if (gatewayId === null || gatewayId === undefined)
        {
            throw new Error('Required parameter gatewayId was null or undefined when calling getBindings.');
        }

        const __path = this.getBindings__generateUrl(gatewayId);

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
    public getDeviceSamplingTemplate__generateUrl(): string
    {
        return this.basePath + '/discovery/device-templates/config';
    }

    /**
     *
     *
     */
    public async getDeviceSamplingTemplate(): Promise<models.DevicesSamplingTemplate>
    {

        const __path = this.getDeviceSamplingTemplate__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.DevicesSamplingTemplate> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DevicesSamplingTemplate.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param networkId
    */
    public getReverseBindings__generateUrl(networkId: string): string
    {
        return this.basePath + '/discovery/reverse-bindings/${networkId}'
                   .replace('${' + 'networkId' + '}', encodeURIComponent(String(networkId)));
    }

    /**
     *
     *
     * @param networkId
     */
    public async getReverseBindings(networkId: string): Promise<models.RecordIdentity>
    {
        // verify required parameter 'networkId' is not null or undefined
        if (networkId === null || networkId === undefined)
        {
            throw new Error('Required parameter networkId was null or undefined when calling getReverseBindings.');
        }

        const __path = this.getReverseBindings__generateUrl(networkId);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

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
    public pushToAzureDigitalTwin__generateUrl(): string
    {
        return this.basePath + '/discovery/azure-digital-twin/publish';
    }

    /**
     *
     *
     */
    public pushToAzureDigitalTwin(): Promise<string>
    {

        const __path = this.pushToAzureDigitalTwin__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('text/plain');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public reclassify__generateUrl(): string
    {
        return this.basePath + '/discovery/device-templates/reclassify';
    }

    /**
     *
     *
     */
    public reclassify(): Promise<void>
    {

        const __path = this.reclassify__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.method = "PUT";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public setAzureDigitalTwinCredentials__generateUrl(): string
    {
        return this.basePath + '/discovery/azure-digital-twin/cred';
    }

    /**
     *
     *
     * @param body
     */
    public async setAzureDigitalTwinCredentials(body?: models.AzureDigitalTwinsHelperCredentials): Promise<models.AzureDigitalTwinsHelperCredentials>
    {

        const __path = this.setAzureDigitalTwinCredentials__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <models.AzureDigitalTwinsHelperCredentials> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.AzureDigitalTwinsHelperCredentials.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public setAzureIoTHubCredentials__generateUrl(): string
    {
        return this.basePath + '/discovery/azure-iot-hub/cred';
    }

    /**
     *
     *
     * @param body
     */
    public async setAzureIoTHubCredentials(body?: models.AzureIotHubHelperCredentials): Promise<models.AzureIotHubHelperCredentials>
    {

        const __path = this.setAzureIoTHubCredentials__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <models.AzureIotHubHelperCredentials> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.AzureIotHubHelperCredentials.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public setDeviceSamplingTemplate__generateUrl(): string
    {
        return this.basePath + '/discovery/device-templates/config';
    }

    /**
     *
     *
     * @param body
     */
    public async setDeviceSamplingTemplate(body?: models.DevicesSamplingTemplate): Promise<models.DevicesSamplingTemplate>
    {

        const __path = this.setDeviceSamplingTemplate__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <models.DevicesSamplingTemplate> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DevicesSamplingTemplate.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public startReport__generateUrl(): string
    {
        return this.basePath + '/discovery/report/start';
    }

    /**
     *
     *
     * @param body
     */
    public startReport(body?: models.DiscoveryReportRun): Promise<string>
    {

        const __path = this.startReport__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('text/plain');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    * @param id
    * @param fileName
    */
    public streamReport__generateUrl(id: string, fileName: string): string
    {
        return this.basePath + '/discovery/report/excel/${id}/${fileName}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)))
                   .replace('${' + 'fileName' + '}', encodeURIComponent(String(fileName)));
    }

    /**
     *
     *
     * @param id
     * @param fileName
     */
    public streamReport(id: string, fileName: string): Promise<Blob>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling streamReport.');
        }
        // verify required parameter 'fileName' is not null or undefined
        if (fileName === null || fileName === undefined)
        {
            throw new Error('Required parameter fileName was null or undefined when calling streamReport.');
        }

        const __path = this.streamReport__generateUrl(id, fileName);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/octet-stream');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    * @param gatewayId
    * @param networkId
    */
    public unbind__generateUrl(gatewayId: string, networkId: string): string
    {
        return this.basePath + '/discovery/bindings/${gatewayId}/remove/${networkId}'
                   .replace('${' + 'gatewayId' + '}', encodeURIComponent(String(gatewayId)))
                   .replace('${' + 'networkId' + '}', encodeURIComponent(String(networkId)));
    }

    /**
     *
     *
     * @param gatewayId
     * @param networkId
     */
    public unbind(gatewayId: string, networkId: string): Promise<boolean>
    {
        // verify required parameter 'gatewayId' is not null or undefined
        if (gatewayId === null || gatewayId === undefined)
        {
            throw new Error('Required parameter gatewayId was null or undefined when calling unbind.');
        }
        // verify required parameter 'networkId' is not null or undefined
        if (networkId === null || networkId === undefined)
        {
            throw new Error('Required parameter networkId was null or undefined when calling unbind.');
        }

        const __path = this.unbind__generateUrl(gatewayId, networkId);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    * @param networkId
    */
    public updateSampling__generateUrl(networkId: string): string
    {
        return this.basePath + '/discovery/sampling/${networkId}/refresh'
                   .replace('${' + 'networkId' + '}', encodeURIComponent(String(networkId)));
    }

    /**
     *
     *
     * @param networkId
     * @param dryRun
     * @param startWithClassId
     * @param stopWithoutClassId
     * @param triggerConfiguration
     */
    public async updateSampling(networkId: string, dryRun?: boolean, startWithClassId?: boolean, stopWithoutClassId?: boolean, triggerConfiguration?: boolean): Promise<models.RecordIdentity>
    {
        // verify required parameter 'networkId' is not null or undefined
        if (networkId === null || networkId === undefined)
        {
            throw new Error('Required parameter networkId was null or undefined when calling updateSampling.');
        }

        const __path = this.updateSampling__generateUrl(networkId);

        let __requestOptions = new ApiRequest();

        if (dryRun !== undefined)
        {
            __requestOptions.setQueryParam('dryRun', <any>dryRun);
        }

        if (startWithClassId !== undefined)
        {
            __requestOptions.setQueryParam('startWithClassId', <any>startWithClassId);
        }

        if (stopWithoutClassId !== undefined)
        {
            __requestOptions.setQueryParam('stopWithoutClassId', <any>stopWithoutClassId);
        }

        if (triggerConfiguration !== undefined)
        {
            __requestOptions.setQueryParam('triggerConfiguration', <any>triggerConfiguration);
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

}
