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

export class EngineLiteralFromCore extends models.EngineLiteral {
    getFixupPrototypeFunction() { return EngineLiteralFromCore.fixupPrototype; }

    static newInstance(model: Partial<EngineLiteralFromCore>): EngineLiteralFromCore {
        let obj = Object.assign(new EngineLiteralFromCore(), model);
        EngineLiteralFromCore.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineLiteralFromCore>): EngineLiteralFromCore {
        if (!model) return null;
        return EngineLiteralFromCore.newInstance(<EngineLiteralFromCore> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineLiteralFromCore) {
        models.EngineLiteral.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
