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

export class ProberForeignDeviceTableEntry {
    getFixupPrototypeFunction() { return ProberForeignDeviceTableEntry.fixupPrototype; }

    static newInstance(model: Partial<ProberForeignDeviceTableEntry>): ProberForeignDeviceTableEntry {
        let obj = Object.assign(new ProberForeignDeviceTableEntry(), model);
        ProberForeignDeviceTableEntry.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ProberForeignDeviceTableEntry>): ProberForeignDeviceTableEntry {
        if (!model) return null;
        return ProberForeignDeviceTableEntry.newInstance(<ProberForeignDeviceTableEntry> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ProberForeignDeviceTableEntry) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.ProberForeignDeviceTableEntry.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.timeToLive === undefined) {
            this.timeToLive = 0;
        }
        if (this.remainingTimeToLive === undefined) {
            this.remainingTimeToLive = 0;
        }
    }

    address: string;

    timeToLive: number;

    remainingTimeToLive: number;

}
