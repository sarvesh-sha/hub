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

export class SensorResultForBluesky extends models.SensorResult {
    constructor() {
        super();
        this.setDiscriminator("SensorResultForBluesky");
    }

    getFixupPrototypeFunction() { return SensorResultForBluesky.fixupPrototype; }

    static newInstance(model: Partial<SensorResultForBluesky>): SensorResultForBluesky {
        let obj = Object.assign(new SensorResultForBluesky(), model);
        SensorResultForBluesky.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<SensorResultForBluesky>): SensorResultForBluesky {
        if (!model) return null;
        return SensorResultForBluesky.newInstance(<SensorResultForBluesky> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: SensorResultForBluesky) {
        models.SensorResult.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.inputVoltage === undefined) {
            this.inputVoltage = 0;
        }
        if (this.inputCurrent === undefined) {
            this.inputCurrent = 0;
        }
        if (this.batteryVoltage === undefined) {
            this.batteryVoltage = 0;
        }
        if (this.batteryCurrent === undefined) {
            this.batteryCurrent = 0;
        }
        if (this.totalChargeAH === undefined) {
            this.totalChargeAH = 0;
        }
    }

    inputVoltage: number;

    inputCurrent: number;

    batteryVoltage: number;

    batteryCurrent: number;

    totalChargeAH: number;

}
