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
export class CustomersApi
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
        return this.basePath + '/customers/create';
    }

    /**
     *
     *
     * @param body
     */
    public async create(body?: models.Customer): Promise<models.Customer>
    {

        const __path = this.create__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <models.Customer> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.Customer.fixupPrototype(__res);
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
        return this.basePath + '/customers/item/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     */
    public async get(id: string): Promise<models.Customer>
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

        let __res = <models.Customer> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.Customer.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    */
    public getAll__generateUrl(): string
    {
        return this.basePath + '/customers/all';
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
    public getAllCharges__generateUrl(): string
    {
        return this.basePath + '/customers/charges';
    }

    /**
     *
     *
     * @param maxTopHosts
     */
    public async getAllCharges(maxTopHosts?: number): Promise<models.DeploymentCellularChargesSummary>
    {

        const __path = this.getAllCharges__generateUrl();

        let __requestOptions = new ApiRequest();

        if (maxTopHosts !== undefined)
        {
            __requestOptions.setQueryParam('maxTopHosts', <any>maxTopHosts);
        }

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.DeploymentCellularChargesSummary> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DeploymentCellularChargesSummary.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param fileName
    */
    public getAllChargesReport__generateUrl(fileName: string): string
    {
        return this.basePath + '/customers/charges-report/${fileName}'
                   .replace('${' + 'fileName' + '}', encodeURIComponent(String(fileName)));
    }

    /**
     *
     *
     * @param fileName
     */
    public getAllChargesReport(fileName: string): Promise<string>
    {
        // verify required parameter 'fileName' is not null or undefined
        if (fileName === null || fileName === undefined)
        {
            throw new Error('Required parameter fileName was null or undefined when calling getAllChargesReport.');
        }

        const __path = this.getAllChargesReport__generateUrl(fileName);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/csv');
        __requestOptions.method = "GET";

        return this.client.callWithOptions(__path, __requestOptions);
    }

    /**
    *
    *
    */
    public getBatch__generateUrl(): string
    {
        return this.basePath + '/customers/batch';
    }

    /**
     *
     *
     * @param body
     */
    public async getBatch(body?: Array<string>): Promise<Array<models.Customer>>
    {

        const __path = this.getBatch__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <Array<models.Customer>> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            for (let val of __res)
            {
                models.Customer.fixupPrototype(val);
            }
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    */
    public getCharges__generateUrl(id: string): string
    {
        return this.basePath + '/customers/item/${id}/charges'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param maxTopHosts
     */
    public async getCharges(id: string, maxTopHosts?: number): Promise<models.DeploymentCellularChargesSummary>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling getCharges.');
        }

        const __path = this.getCharges__generateUrl(id);

        let __requestOptions = new ApiRequest();

        if (maxTopHosts !== undefined)
        {
            __requestOptions.setQueryParam('maxTopHosts', <any>maxTopHosts);
        }

        __requestOptions.setProduce('application/json');
        __requestOptions.method = "GET";

        let __res = <models.DeploymentCellularChargesSummary> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.DeploymentCellularChargesSummary.fixupPrototype(__res);
        }

        return __res;
    }

    /**
    *
    *
    * @param id
    * @param fileName
    */
    public getChargesReport__generateUrl(id: string, fileName: string): string
    {
        return this.basePath + '/customers/item/${id}/charges-report/${fileName}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)))
                   .replace('${' + 'fileName' + '}', encodeURIComponent(String(fileName)));
    }

    /**
     *
     *
     * @param id
     * @param fileName
     */
    public getChargesReport(id: string, fileName: string): Promise<string>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling getChargesReport.');
        }
        // verify required parameter 'fileName' is not null or undefined
        if (fileName === null || fileName === undefined)
        {
            throw new Error('Required parameter fileName was null or undefined when calling getChargesReport.');
        }

        const __path = this.getChargesReport__generateUrl(id, fileName);

        let __requestOptions = new ApiRequest();

        __requestOptions.setProduce('application/csv');
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
        return this.basePath + '/customers/item/${id}'
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
    public update__generateUrl(id: string): string
    {
        return this.basePath + '/customers/item/${id}'
                   .replace('${' + 'id' + '}', encodeURIComponent(String(id)));
    }

    /**
     *
     *
     * @param id
     * @param dryRun
     * @param body
     */
    public async update(id: string, dryRun?: boolean, body?: models.Customer): Promise<models.ValidationResults>
    {
        // verify required parameter 'id' is not null or undefined
        if (id === null || id === undefined)
        {
            throw new Error('Required parameter id was null or undefined when calling update.');
        }

        const __path = this.update__generateUrl(id);

        let __requestOptions = new ApiRequest();

        if (dryRun !== undefined)
        {
            __requestOptions.setQueryParam('dryRun', <any>dryRun);
        }

        __requestOptions.setConsume('application/json');
        __requestOptions.setProduce('application/json');
        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        let __res = <models.ValidationResults> await this.client.callWithOptions(__path, __requestOptions);
        if (__res)
        {
            models.ValidationResults.fixupPrototype(__res);
        }

        return __res;
    }

}
