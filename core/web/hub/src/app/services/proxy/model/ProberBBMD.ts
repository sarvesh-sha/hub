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

export class ProberBBMD {
    getFixupPrototypeFunction() { return ProberBBMD.fixupPrototype; }

    static newInstance(model: Partial<ProberBBMD>): ProberBBMD {
        let obj = Object.assign(new ProberBBMD(), model);
        ProberBBMD.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ProberBBMD>): ProberBBMD {
        if (!model) return null;
        return ProberBBMD.newInstance(<ProberBBMD> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ProberBBMD) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.ProberBBMD.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.descriptor) {
            models.BACnetBBMD.fixupPrototype(this.descriptor);
        }
        if (this.bdt) {
            for (let val of this.bdt) {
                models.ProberBroadcastDistributionTableEntry.fixupPrototype(val);
            }
        }
        if (this.fdt) {
            for (let val of this.fdt) {
                models.ProberForeignDeviceTableEntry.fixupPrototype(val);
            }
        }
    }

    descriptor: models.BACnetBBMD;

    bdt: Array<models.ProberBroadcastDistributionTableEntry>;

    fdt: Array<models.ProberForeignDeviceTableEntry>;

}