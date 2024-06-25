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

export class AlertEngineOperatorUnaryControlPointLastSample extends models.EngineOperatorUnaryFromAlerts {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineOperatorUnaryControlPointLastSample");
    }

    getFixupPrototypeFunction() { return AlertEngineOperatorUnaryControlPointLastSample.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineOperatorUnaryControlPointLastSample>): AlertEngineOperatorUnaryControlPointLastSample {
        let obj = Object.assign(new AlertEngineOperatorUnaryControlPointLastSample(), model);
        AlertEngineOperatorUnaryControlPointLastSample.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineOperatorUnaryControlPointLastSample>): AlertEngineOperatorUnaryControlPointLastSample {
        if (!model) return null;
        return AlertEngineOperatorUnaryControlPointLastSample.newInstance(<AlertEngineOperatorUnaryControlPointLastSample> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineOperatorUnaryControlPointLastSample) {
        models.EngineOperatorUnaryFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}