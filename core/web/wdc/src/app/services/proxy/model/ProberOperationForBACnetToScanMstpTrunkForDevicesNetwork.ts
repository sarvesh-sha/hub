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

export class ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork {
    getFixupPrototypeFunction() { return ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork.fixupPrototype; }

    static newInstance(model: Partial<ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork>): ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork {
        let obj = Object.assign(new ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork(), model);
        ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork>): ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork {
        if (!model) return null;
        return ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork.newInstance(<ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.networkNumber === undefined) {
            this.networkNumber = 0;
        }
        if (this.transport) {
            models.TransportAddress.fixupPrototype(this.transport);
        }
    }

    transport: models.TransportAddress;

    networkNumber: number;

}