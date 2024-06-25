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

export class ProberOperationForBACnetToScanSubnetForDevicesResults extends models.ProberOperationForBACnetBaseResults {
    constructor() {
        super();
        this.setDiscriminator("ProberOperationForBACnetToScanSubnetForDevicesResults");
    }

    getFixupPrototypeFunction() { return ProberOperationForBACnetToScanSubnetForDevicesResults.fixupPrototype; }

    static newInstance(model: Partial<ProberOperationForBACnetToScanSubnetForDevicesResults>): ProberOperationForBACnetToScanSubnetForDevicesResults {
        let obj = Object.assign(new ProberOperationForBACnetToScanSubnetForDevicesResults(), model);
        ProberOperationForBACnetToScanSubnetForDevicesResults.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ProberOperationForBACnetToScanSubnetForDevicesResults>): ProberOperationForBACnetToScanSubnetForDevicesResults {
        if (!model) return null;
        return ProberOperationForBACnetToScanSubnetForDevicesResults.newInstance(<ProberOperationForBACnetToScanSubnetForDevicesResults> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ProberOperationForBACnetToScanSubnetForDevicesResults) {
        models.ProberOperationForBACnetBaseResults.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.discoveredDevices) {
            for (let val of this.discoveredDevices) {
                models.BaseAssetDescriptor.fixupPrototype(val);
            }
        }
    }

    discoveredDevices: Array<models.BACnetDeviceDescriptor>;

}