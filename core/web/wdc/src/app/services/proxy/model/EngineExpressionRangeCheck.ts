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

export class EngineExpressionRangeCheck extends models.EngineExpressionFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineExpressionRangeCheck");
    }

    getFixupPrototypeFunction() { return EngineExpressionRangeCheck.fixupPrototype; }

    static newInstance(model: Partial<EngineExpressionRangeCheck>): EngineExpressionRangeCheck {
        let obj = Object.assign(new EngineExpressionRangeCheck(), model);
        EngineExpressionRangeCheck.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineExpressionRangeCheck>): EngineExpressionRangeCheck {
        if (!model) return null;
        return EngineExpressionRangeCheck.newInstance(<EngineExpressionRangeCheck> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineExpressionRangeCheck) {
        models.EngineExpressionFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.value) {
            models.EngineBlock.fixupPrototype(this.value);
        }
        if (this.minRange) {
            models.EngineBlock.fixupPrototype(this.minRange);
        }
        if (this.maxRange) {
            models.EngineBlock.fixupPrototype(this.maxRange);
        }
    }

    value: models.EngineExpression;

    minRange: models.EngineExpression;

    maxRange: models.EngineExpression;

}
