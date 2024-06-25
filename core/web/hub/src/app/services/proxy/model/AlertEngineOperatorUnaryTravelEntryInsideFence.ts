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

export class AlertEngineOperatorUnaryTravelEntryInsideFence extends models.EngineOperatorUnaryFromAlerts {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineOperatorUnaryTravelEntryInsideFence");
    }

    getFixupPrototypeFunction() { return AlertEngineOperatorUnaryTravelEntryInsideFence.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineOperatorUnaryTravelEntryInsideFence>): AlertEngineOperatorUnaryTravelEntryInsideFence {
        let obj = Object.assign(new AlertEngineOperatorUnaryTravelEntryInsideFence(), model);
        AlertEngineOperatorUnaryTravelEntryInsideFence.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineOperatorUnaryTravelEntryInsideFence>): AlertEngineOperatorUnaryTravelEntryInsideFence {
        if (!model) return null;
        return AlertEngineOperatorUnaryTravelEntryInsideFence.newInstance(<AlertEngineOperatorUnaryTravelEntryInsideFence> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineOperatorUnaryTravelEntryInsideFence) {
        models.EngineOperatorUnaryFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
