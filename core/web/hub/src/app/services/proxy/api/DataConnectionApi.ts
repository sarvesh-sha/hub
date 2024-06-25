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
export class DataConnectionApi
{
    constructor(private client: ApiClient, protected basePath: string)
    {
    }

    /**
    *
    *
    * @param controllerId
    */
    public controllerMetadataAggregation__generateUrl(controllerId: string): string
    {
        return this.basePath + '/data-connection/metadata-aggregation/controller/${controllerId}'
                   .replace('${' + 'controllerId' + '}', encodeURIComponent(String(controllerId)));
    }

    /**
     *
     *
     * @param controllerId
     * @param unclassified
     * @param belowThresholdId
     */
    public async controllerMetadataAggregation(controllerId: string, unclassified?: boolean, belowThresholdId?: number): Promise<models.ControllerMetadataAggregation>
    {
        // verify required parameter 'controllerId' is not null or undefined
        if (controllerId === null || controllerId === undefined)
        {
            throw new Error('Required parameter controllerId was null or undefined when calling controllerMetadataAggregation.');
        }

        const __path = this.controllerMetadataAggregation__generateUrl(controllerId);

        let __requestOptions = new ApiRequest();

        if (unclassified !== undefined)
        {
            __requestOptions.setQueryParam('unclassified', <any>unclassified);
        }

        if (belowThresholdId !== undefined)
        {
            __requestOptions.setQueryParam('belowThresholdId', <any>belowThresholdId);
        }

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.ControllerMetadataAggregation> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.ControllerMetadataAggregation.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public equipmentAggregation__generateUrl(): string
    {
        return this.basePath + '/data-connection/equipment-aggregation';
    }

    /**
     *
     *
     */
    public async equipmentAggregation(): Promise<models.EquipmentAggregation>
    {

        const __path = this.equipmentAggregation__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.EquipmentAggregation> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.EquipmentAggregation.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param connectionId
    * @param controlPointId
    */
    public getLastSample__generateUrl(connectionId: string, controlPointId: string): string
    {
        return this.basePath + '/data-connection/connection/${connectionId}/last-sample/${controlPointId}'
                   .replace('${' + 'connectionId' + '}', encodeURIComponent(String(connectionId)))
                   .replace('${' + 'controlPointId' + '}', encodeURIComponent(String(controlPointId)));
    }

    /**
     *
     *
     * @param connectionId
     * @param controlPointId
     */
    public getLastSample(connectionId: string, controlPointId: string): Promise<Date>
    {
        // verify required parameter 'connectionId' is not null or undefined
        if (connectionId === null || connectionId === undefined)
        {
            throw new Error('Required parameter connectionId was null or undefined when calling getLastSample.');
        }
        // verify required parameter 'controlPointId' is not null or undefined
        if (controlPointId === null || controlPointId === undefined)
        {
            throw new Error('Required parameter controlPointId was null or undefined when calling getLastSample.');
        }

        const __path = this.getLastSample__generateUrl(connectionId, controlPointId);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public getSite__generateUrl(): string
    {
        return this.basePath + '/data-connection/site';
    }

    /**
     *
     *
     */
    public async getSite(): Promise<models.DataConnectionSite>
    {

        const __path = this.getSite__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.DataConnectionSite> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DataConnectionSite.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public metadataAggregation__generateUrl(): string
    {
        return this.basePath + '/data-connection/metadata-aggregation';
    }

    /**
     *
     *
     * @param unclassified
     */
    public async metadataAggregation(unclassified?: boolean): Promise<models.MetadataAggregation>
    {

        const __path = this.metadataAggregation__generateUrl();

        let __requestOptions = new ApiRequest();

        if (unclassified !== undefined)
        {
            __requestOptions.setQueryParam('unclassified', <any>unclassified);
        }

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.MetadataAggregation> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.MetadataAggregation.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param endpointId
    */
    public receiveRaw__generateUrl(endpointId: string): string
    {
        return this.basePath + '/data-connection/endpoint/${endpointId}'
                   .replace('${' + 'endpointId' + '}', encodeURIComponent(String(endpointId)));
    }

    /**
     *
     *
     * @param endpointId
     * @param arg
     * @param body
     */
    public receiveRaw(endpointId: string, arg?: string, body?: any): Promise<any>
    {
        // verify required parameter 'endpointId' is not null or undefined
        if (endpointId === null || endpointId === undefined)
        {
            throw new Error('Required parameter endpointId was null or undefined when calling receiveRaw.');
        }

        const __path = this.receiveRaw__generateUrl(endpointId);

        let __requestOptions = new ApiRequest();

        if (arg !== undefined)
        {
            __requestOptions.setQueryParam('arg', <any>arg);
        }

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    * @param endpointId
    */
    public receiveStream__generateUrl(endpointId: string): string
    {
        return this.basePath + '/data-connection/endpoint/${endpointId}/stream'
                   .replace('${' + 'endpointId' + '}', encodeURIComponent(String(endpointId)));
    }

    /**
     *
     *
     * @param endpointId
     * @param arg
     * @param body
     */
    public receiveStream(endpointId: string, arg?: string, body?: Blob): Promise<any>
    {
        // verify required parameter 'endpointId' is not null or undefined
        if (endpointId === null || endpointId === undefined)
        {
            throw new Error('Required parameter endpointId was null or undefined when calling receiveStream.');
        }

        const __path = this.receiveStream__generateUrl(endpointId);

        let __requestOptions = new ApiRequest();

        if (arg !== undefined)
        {
            __requestOptions.setQueryParam('arg', <any>arg);
        }

        __requestOptions.setConsume('application/octet-stream');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    * @param connectionId
    * @param controlPointId
    */
    public setLastSample__generateUrl(connectionId: string, controlPointId: string): string
    {
        return this.basePath + '/data-connection/connection/${connectionId}/last-sample/${controlPointId}'
                   .replace('${' + 'connectionId' + '}', encodeURIComponent(String(connectionId)))
                   .replace('${' + 'controlPointId' + '}', encodeURIComponent(String(controlPointId)));
    }

    /**
     *
     *
     * @param connectionId
     * @param controlPointId
     * @param body
     */
    public setLastSample(connectionId: string, controlPointId: string, body?: Date): Promise<Date>
    {
        // verify required parameter 'connectionId' is not null or undefined
        if (connectionId === null || connectionId === undefined)
        {
            throw new Error('Required parameter connectionId was null or undefined when calling setLastSample.');
        }
        // verify required parameter 'controlPointId' is not null or undefined
        if (controlPointId === null || controlPointId === undefined)
        {
            throw new Error('Required parameter controlPointId was null or undefined when calling setLastSample.');
        }

        const __path = this.setLastSample__generateUrl(connectionId, controlPointId);

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public setSite__generateUrl(): string
    {
        return this.basePath + '/data-connection/site';
    }

    /**
     *
     *
     * @param body
     */
    public async setSite(body?: models.DataConnectionSite): Promise<models.DataConnectionSite>
    {

        const __path = this.setSite__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <models.DataConnectionSite> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DataConnectionSite.fixupPrototype(__res);
        }

        return __res;
    }

}