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

export class AlertEngineOperatorUnaryControlPointNewSamples extends models.EngineOperatorUnaryFromAlerts {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineOperatorUnaryControlPointNewSamples");
    }

    getFixupPrototypeFunction() { return AlertEngineOperatorUnaryControlPointNewSamples.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineOperatorUnaryControlPointNewSamples>): AlertEngineOperatorUnaryControlPointNewSamples {
        let obj = Object.assign(new AlertEngineOperatorUnaryControlPointNewSamples(), model);
        AlertEngineOperatorUnaryControlPointNewSamples.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineOperatorUnaryControlPointNewSamples>): AlertEngineOperatorUnaryControlPointNewSamples {
        if (!model) return null;
        return AlertEngineOperatorUnaryControlPointNewSamples.newInstance(<AlertEngineOperatorUnaryControlPointNewSamples> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineOperatorUnaryControlPointNewSamples) {
        models.EngineOperatorUnaryFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
