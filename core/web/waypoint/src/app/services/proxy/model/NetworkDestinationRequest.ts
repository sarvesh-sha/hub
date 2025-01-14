/**
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Waypoint APIs
 * APIs and Definitions for the Optio3 Waypoint product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

import * as models from './models';

export class NetworkDestinationRequest {
    getFixupPrototypeFunction() { return NetworkDestinationRequest.fixupPrototype; }

    static newInstance(model: Partial<NetworkDestinationRequest>): NetworkDestinationRequest {
        let obj = Object.assign(new NetworkDestinationRequest(), model);
        NetworkDestinationRequest.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NetworkDestinationRequest>): NetworkDestinationRequest {
        if (!model) return null;
        return NetworkDestinationRequest.newInstance(<NetworkDestinationRequest> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NetworkDestinationRequest) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.NetworkDestinationRequest.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    url: string;

}
