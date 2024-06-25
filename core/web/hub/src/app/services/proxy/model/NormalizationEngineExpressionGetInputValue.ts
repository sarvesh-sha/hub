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

export class NormalizationEngineExpressionGetInputValue extends models.EngineExpressionFromNormalization {
    constructor() {
        super();
        this.setDiscriminator("NormalizationEngineExpressionGetInputValue");
    }

    getFixupPrototypeFunction() { return NormalizationEngineExpressionGetInputValue.fixupPrototype; }

    static newInstance(model: Partial<NormalizationEngineExpressionGetInputValue>): NormalizationEngineExpressionGetInputValue {
        let obj = Object.assign(new NormalizationEngineExpressionGetInputValue(), model);
        NormalizationEngineExpressionGetInputValue.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationEngineExpressionGetInputValue>): NormalizationEngineExpressionGetInputValue {
        if (!model) return null;
        return NormalizationEngineExpressionGetInputValue.newInstance(<NormalizationEngineExpressionGetInputValue> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationEngineExpressionGetInputValue) {
        models.EngineExpressionFromNormalization.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
