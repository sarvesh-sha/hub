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

export class SensorResultForRawCANbus extends models.SensorResult {
    constructor() {
        super();
        this.setDiscriminator("SensorResultForRawCANbus");
    }

    getFixupPrototypeFunction() { return SensorResultForRawCANbus.fixupPrototype; }

    static newInstance(model: Partial<SensorResultForRawCANbus>): SensorResultForRawCANbus {
        let obj = Object.assign(new SensorResultForRawCANbus(), model);
        SensorResultForRawCANbus.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<SensorResultForRawCANbus>): SensorResultForRawCANbus {
        if (!model) return null;
        return SensorResultForRawCANbus.newInstance(<SensorResultForRawCANbus> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: SensorResultForRawCANbus) {
        models.SensorResult.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    found: { [key: string]: number; };

}
