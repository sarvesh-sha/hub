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

export class EngineExpressionMemoize extends models.EngineExpressionFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineExpressionMemoize");
    }

    getFixupPrototypeFunction() { return EngineExpressionMemoize.fixupPrototype; }

    static newInstance(model: Partial<EngineExpressionMemoize>): EngineExpressionMemoize {
        let obj = Object.assign(new EngineExpressionMemoize(), model);
        EngineExpressionMemoize.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineExpressionMemoize>): EngineExpressionMemoize {
        if (!model) return null;
        return EngineExpressionMemoize.newInstance(<EngineExpressionMemoize> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineExpressionMemoize) {
        models.EngineExpressionFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.value) {
            models.EngineBlock.fixupPrototype(this.value);
        }
    }

    value: models.EngineExpression;

}
