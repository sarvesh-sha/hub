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

import * as models from './models';

export class ProvisionFirmware {
    getFixupPrototypeFunction() { return ProvisionFirmware.fixupPrototype; }

    static newInstance(model: Partial<ProvisionFirmware>): ProvisionFirmware {
        let obj = Object.assign(new ProvisionFirmware(), model);
        ProvisionFirmware.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ProvisionFirmware>): ProvisionFirmware {
        if (!model) return null;
        return ProvisionFirmware.newInstance(<ProvisionFirmware> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ProvisionFirmware) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.ProvisionFirmware.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.size === undefined) {
            this.size = 0;
        }
        if (typeof this.timestamp === "string") {
            this.timestamp = new Date(<string><any>this.timestamp);
        }
    }

    timestamp: Date;

    name: string;

    size: number;

}