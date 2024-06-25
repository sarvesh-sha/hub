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

export class EngineLiteral extends models.EngineExpressionFromCore {
    getFixupPrototypeFunction() { return EngineLiteral.fixupPrototype; }

    static newInstance(model: Partial<EngineLiteral>): EngineLiteral {
        let obj = Object.assign(new EngineLiteral(), model);
        EngineLiteral.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineLiteral>): EngineLiteral {
        if (!model) return null;
        return EngineLiteral.newInstance(<EngineLiteral> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineLiteral) {
        models.EngineExpressionFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}