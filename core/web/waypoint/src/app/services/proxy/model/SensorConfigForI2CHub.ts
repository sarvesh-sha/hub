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

export class SensorConfigForI2CHub extends models.SensorConfig {
    constructor() {
        super();
        this.setDiscriminator("SensorConfigForI2CHub");
    }

    getFixupPrototypeFunction() { return SensorConfigForI2CHub.fixupPrototype; }

    static newInstance(model: Partial<SensorConfigForI2CHub>): SensorConfigForI2CHub {
        let obj = Object.assign(new SensorConfigForI2CHub(), model);
        SensorConfigForI2CHub.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<SensorConfigForI2CHub>): SensorConfigForI2CHub {
        if (!model) return null;
        return SensorConfigForI2CHub.newInstance(<SensorConfigForI2CHub> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: SensorConfigForI2CHub) {
        models.SensorConfig.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
