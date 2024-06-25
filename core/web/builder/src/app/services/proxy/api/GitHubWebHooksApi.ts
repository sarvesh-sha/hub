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
export class GitHubWebHooksApi
{
    constructor(private client: ApiClient, protected basePath: string)
    {
    }

    /**
    *
    *
    */
    public postToHook__generateUrl(): string
    {
        return this.basePath + '/github/webhooks';
    }

    /**
     *
     *
     * @param xGitHubEvent
     * @param xGitHubDelivery
     * @param xHubSignature
     * @param body
     */
    public postToHook(xGitHubEvent?: string, xGitHubDelivery?: string, xHubSignature?: string, body?: Array<string>): Promise<string>
    {

        const __path = this.postToHook__generateUrl();

        let __requestOptions = new ApiRequest();

        __requestOptions.setHeader('X-GitHub-Event', String(xGitHubEvent));

        __requestOptions.setHeader('X-GitHub-Delivery', String(xGitHubDelivery));

        __requestOptions.setHeader('X-Hub-Signature', String(xHubSignature));

        __requestOptions.body = body;
        __requestOptions.hasBodyParam = true;
        __requestOptions.method = "POST";

        return this.client.callWithOptions(__path, __requestOptions);
    }

}