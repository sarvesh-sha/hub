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

export class EngineOperatorUnaryFromMetrics extends models.EngineOperatorUnary {
    getFixupPrototypeFunction() { return EngineOperatorUnaryFromMetrics.fixupPrototype; }

    static newInstance(model: Partial<EngineOperatorUnaryFromMetrics>): EngineOperatorUnaryFromMetrics {
        let obj = Object.assign(new EngineOperatorUnaryFromMetrics(), model);
        EngineOperatorUnaryFromMetrics.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineOperatorUnaryFromMetrics>): EngineOperatorUnaryFromMetrics {
        if (!model) return null;
        return EngineOperatorUnaryFromMetrics.newInstance(<EngineOperatorUnaryFromMetrics> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineOperatorUnaryFromMetrics) {
        models.EngineOperatorUnary.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
