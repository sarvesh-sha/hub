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

export class EngineExpressionBinaryStringEndsWith extends models.EngineOperatorBinaryFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineExpressionBinaryStringEndsWith");
    }

    getFixupPrototypeFunction() { return EngineExpressionBinaryStringEndsWith.fixupPrototype; }

    static newInstance(model: Partial<EngineExpressionBinaryStringEndsWith>): EngineExpressionBinaryStringEndsWith {
        let obj = Object.assign(new EngineExpressionBinaryStringEndsWith(), model);
        EngineExpressionBinaryStringEndsWith.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineExpressionBinaryStringEndsWith>): EngineExpressionBinaryStringEndsWith {
        if (!model) return null;
        return EngineExpressionBinaryStringEndsWith.newInstance(<EngineExpressionBinaryStringEndsWith> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineExpressionBinaryStringEndsWith) {
        models.EngineOperatorBinaryFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}