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

export class AlertEngineOperatorUnarySampleGetTime extends models.EngineOperatorUnaryFromAlerts {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineOperatorUnarySampleGetTime");
    }

    getFixupPrototypeFunction() { return AlertEngineOperatorUnarySampleGetTime.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineOperatorUnarySampleGetTime>): AlertEngineOperatorUnarySampleGetTime {
        let obj = Object.assign(new AlertEngineOperatorUnarySampleGetTime(), model);
        AlertEngineOperatorUnarySampleGetTime.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineOperatorUnarySampleGetTime>): AlertEngineOperatorUnarySampleGetTime {
        if (!model) return null;
        return AlertEngineOperatorUnarySampleGetTime.newInstance(<AlertEngineOperatorUnarySampleGetTime> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineOperatorUnarySampleGetTime) {
        models.EngineOperatorUnaryFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
