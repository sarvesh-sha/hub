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

export class EngineExpressionCurrentDateTime extends models.EngineExpressionFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineExpressionCurrentDateTime");
    }

    getFixupPrototypeFunction() { return EngineExpressionCurrentDateTime.fixupPrototype; }

    static newInstance(model: Partial<EngineExpressionCurrentDateTime>): EngineExpressionCurrentDateTime {
        let obj = Object.assign(new EngineExpressionCurrentDateTime(), model);
        EngineExpressionCurrentDateTime.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineExpressionCurrentDateTime>): EngineExpressionCurrentDateTime {
        if (!model) return null;
        return EngineExpressionCurrentDateTime.newInstance(<EngineExpressionCurrentDateTime> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineExpressionCurrentDateTime) {
        models.EngineExpressionFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
