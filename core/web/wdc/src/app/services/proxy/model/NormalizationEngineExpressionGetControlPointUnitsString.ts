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

export class NormalizationEngineExpressionGetControlPointUnitsString extends models.EngineExpressionFromNormalization {
    constructor() {
        super();
        this.setDiscriminator("NormalizationEngineExpressionGetControlPointUnitsString");
    }

    getFixupPrototypeFunction() { return NormalizationEngineExpressionGetControlPointUnitsString.fixupPrototype; }

    static newInstance(model: Partial<NormalizationEngineExpressionGetControlPointUnitsString>): NormalizationEngineExpressionGetControlPointUnitsString {
        let obj = Object.assign(new NormalizationEngineExpressionGetControlPointUnitsString(), model);
        NormalizationEngineExpressionGetControlPointUnitsString.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationEngineExpressionGetControlPointUnitsString>): NormalizationEngineExpressionGetControlPointUnitsString {
        if (!model) return null;
        return NormalizationEngineExpressionGetControlPointUnitsString.newInstance(<NormalizationEngineExpressionGetControlPointUnitsString> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationEngineExpressionGetControlPointUnitsString) {
        models.EngineExpressionFromNormalization.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
