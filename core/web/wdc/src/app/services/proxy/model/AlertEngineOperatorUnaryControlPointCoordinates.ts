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

export class AlertEngineOperatorUnaryControlPointCoordinates extends models.EngineOperatorUnaryFromAlerts {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineOperatorUnaryControlPointCoordinates");
    }

    getFixupPrototypeFunction() { return AlertEngineOperatorUnaryControlPointCoordinates.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineOperatorUnaryControlPointCoordinates>): AlertEngineOperatorUnaryControlPointCoordinates {
        let obj = Object.assign(new AlertEngineOperatorUnaryControlPointCoordinates(), model);
        AlertEngineOperatorUnaryControlPointCoordinates.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineOperatorUnaryControlPointCoordinates>): AlertEngineOperatorUnaryControlPointCoordinates {
        if (!model) return null;
        return AlertEngineOperatorUnaryControlPointCoordinates.newInstance(<AlertEngineOperatorUnaryControlPointCoordinates> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineOperatorUnaryControlPointCoordinates) {
        models.EngineOperatorUnaryFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}