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

export class NormalizationEngineExpressionGetControlPointLocation extends models.EngineExpressionFromNormalization {
    constructor() {
        super();
        this.setDiscriminator("NormalizationEngineExpressionGetControlPointLocation");
    }

    getFixupPrototypeFunction() { return NormalizationEngineExpressionGetControlPointLocation.fixupPrototype; }

    static newInstance(model: Partial<NormalizationEngineExpressionGetControlPointLocation>): NormalizationEngineExpressionGetControlPointLocation {
        let obj = Object.assign(new NormalizationEngineExpressionGetControlPointLocation(), model);
        NormalizationEngineExpressionGetControlPointLocation.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationEngineExpressionGetControlPointLocation>): NormalizationEngineExpressionGetControlPointLocation {
        if (!model) return null;
        return NormalizationEngineExpressionGetControlPointLocation.newInstance(<NormalizationEngineExpressionGetControlPointLocation> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationEngineExpressionGetControlPointLocation) {
        models.EngineExpressionFromNormalization.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
