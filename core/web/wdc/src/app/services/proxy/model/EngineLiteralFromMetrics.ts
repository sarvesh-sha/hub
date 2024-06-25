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

export class EngineLiteralFromMetrics extends models.EngineLiteral {
    getFixupPrototypeFunction() { return EngineLiteralFromMetrics.fixupPrototype; }

    static newInstance(model: Partial<EngineLiteralFromMetrics>): EngineLiteralFromMetrics {
        let obj = Object.assign(new EngineLiteralFromMetrics(), model);
        EngineLiteralFromMetrics.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineLiteralFromMetrics>): EngineLiteralFromMetrics {
        if (!model) return null;
        return EngineLiteralFromMetrics.newInstance(<EngineLiteralFromMetrics> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineLiteralFromMetrics) {
        models.EngineLiteral.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
