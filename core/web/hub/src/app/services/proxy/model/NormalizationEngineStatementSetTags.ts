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

export class NormalizationEngineStatementSetTags extends models.EngineStatementFromNormalization {
    constructor() {
        super();
        this.setDiscriminator("NormalizationEngineStatementSetTags");
    }

    getFixupPrototypeFunction() { return NormalizationEngineStatementSetTags.fixupPrototype; }

    static newInstance(model: Partial<NormalizationEngineStatementSetTags>): NormalizationEngineStatementSetTags {
        let obj = Object.assign(new NormalizationEngineStatementSetTags(), model);
        NormalizationEngineStatementSetTags.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationEngineStatementSetTags>): NormalizationEngineStatementSetTags {
        if (!model) return null;
        return NormalizationEngineStatementSetTags.newInstance(<NormalizationEngineStatementSetTags> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationEngineStatementSetTags) {
        models.EngineStatementFromNormalization.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.value) {
            models.EngineBlock.fixupPrototype(this.value);
        }
    }

    value: models.EngineExpression;

}
