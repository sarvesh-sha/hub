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

export class BACnetBBMD {
    getFixupPrototypeFunction() { return BACnetBBMD.fixupPrototype; }

    static newInstance(model: Partial<BACnetBBMD>): BACnetBBMD {
        let obj = Object.assign(new BACnetBBMD(), model);
        BACnetBBMD.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<BACnetBBMD>): BACnetBBMD {
        if (!model) return null;
        return BACnetBBMD.newInstance(<BACnetBBMD> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: BACnetBBMD) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.BACnetBBMD.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.networkPort === undefined) {
            this.networkPort = 0;
        }
    }

    networkAddress: string;

    networkPort: number;

    notes: string;

}
