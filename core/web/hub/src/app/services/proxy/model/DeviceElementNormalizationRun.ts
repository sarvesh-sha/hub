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

export class DeviceElementNormalizationRun {
    getFixupPrototypeFunction() { return DeviceElementNormalizationRun.fixupPrototype; }

    static newInstance(model: Partial<DeviceElementNormalizationRun>): DeviceElementNormalizationRun {
        let obj = Object.assign(new DeviceElementNormalizationRun(), model);
        DeviceElementNormalizationRun.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DeviceElementNormalizationRun>): DeviceElementNormalizationRun {
        if (!model) return null;
        return DeviceElementNormalizationRun.newInstance(<DeviceElementNormalizationRun> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DeviceElementNormalizationRun) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DeviceElementNormalizationRun.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.rules) {
            models.NormalizationRules.fixupPrototype(this.rules);
        }
        if (this.devices) {
            for (let val of this.devices) {
                models.RecordIdentity.fixupPrototype(val);
            }
        }
    }

    rulesId: string;

    rules: models.NormalizationRules;

    devices: Array<models.RecordIdentity>;

}
