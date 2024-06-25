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

export class AlertEngineExpressionBinaryControlPointSampleAggregate extends models.EngineOperatorBinaryFromAlerts {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineExpressionBinaryControlPointSampleAggregate");
    }

    getFixupPrototypeFunction() { return AlertEngineExpressionBinaryControlPointSampleAggregate.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineExpressionBinaryControlPointSampleAggregate>): AlertEngineExpressionBinaryControlPointSampleAggregate {
        let obj = Object.assign(new AlertEngineExpressionBinaryControlPointSampleAggregate(), model);
        AlertEngineExpressionBinaryControlPointSampleAggregate.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineExpressionBinaryControlPointSampleAggregate>): AlertEngineExpressionBinaryControlPointSampleAggregate {
        if (!model) return null;
        return AlertEngineExpressionBinaryControlPointSampleAggregate.newInstance(<AlertEngineExpressionBinaryControlPointSampleAggregate> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineExpressionBinaryControlPointSampleAggregate) {
        models.EngineOperatorBinaryFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.unitsFactors) {
            models.EngineeringUnitsFactors.fixupPrototype(this.unitsFactors);
        }
    }

    aggregate: models.AlertSampleAggregate;

    unitsFactors: models.EngineeringUnitsFactors;

}