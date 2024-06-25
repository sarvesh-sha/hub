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

import * as models from './models';

export class ProberOperationForBACnetToReadDevices extends models.ProberOperationForBACnet {
    constructor() {
        super();
        this.setDiscriminator("ProberOperationForBACnetToReadDevices");
    }

    getFixupPrototypeFunction() { return ProberOperationForBACnetToReadDevices.fixupPrototype; }

    static newInstance(model: Partial<ProberOperationForBACnetToReadDevices>): ProberOperationForBACnetToReadDevices {
        let obj = Object.assign(new ProberOperationForBACnetToReadDevices(), model);
        ProberOperationForBACnetToReadDevices.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ProberOperationForBACnetToReadDevices>): ProberOperationForBACnetToReadDevices {
        if (!model) return null;
        return ProberOperationForBACnetToReadDevices.newInstance(<ProberOperationForBACnetToReadDevices> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ProberOperationForBACnetToReadDevices) {
        models.ProberOperationForBACnet.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.targetDevices) {
            for (let val of this.targetDevices) {
                models.BaseAssetDescriptor.fixupPrototype(val);
            }
        }
    }

    targetDevices: Array<models.BACnetDeviceDescriptor>;

}
