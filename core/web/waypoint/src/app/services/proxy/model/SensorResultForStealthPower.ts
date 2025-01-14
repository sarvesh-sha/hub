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

export class SensorResultForStealthPower extends models.SensorResult {
    constructor() {
        super();
        this.setDiscriminator("SensorResultForStealthPower");
    }

    getFixupPrototypeFunction() { return SensorResultForStealthPower.fixupPrototype; }

    static newInstance(model: Partial<SensorResultForStealthPower>): SensorResultForStealthPower {
        let obj = Object.assign(new SensorResultForStealthPower(), model);
        SensorResultForStealthPower.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<SensorResultForStealthPower>): SensorResultForStealthPower {
        if (!model) return null;
        return SensorResultForStealthPower.newInstance(<SensorResultForStealthPower> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: SensorResultForStealthPower) {
        models.SensorResult.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.supply_voltage === undefined) {
            this.supply_voltage = 0;
        }
    }

    supply_voltage: number;

}
