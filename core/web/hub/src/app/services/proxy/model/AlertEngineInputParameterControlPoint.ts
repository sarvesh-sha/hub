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

export class AlertEngineInputParameterControlPoint extends models.EngineInputParameterFromAlerts {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineInputParameterControlPoint");
    }

    getFixupPrototypeFunction() { return AlertEngineInputParameterControlPoint.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineInputParameterControlPoint>): AlertEngineInputParameterControlPoint {
        let obj = Object.assign(new AlertEngineInputParameterControlPoint(), model);
        AlertEngineInputParameterControlPoint.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineInputParameterControlPoint>): AlertEngineInputParameterControlPoint {
        if (!model) return null;
        return AlertEngineInputParameterControlPoint.newInstance(<AlertEngineInputParameterControlPoint> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineInputParameterControlPoint) {
        models.EngineInputParameterFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.value) {
            models.RecordIdentity.fixupPrototype(this.value);
        }
    }

    value: models.RecordIdentity;

}