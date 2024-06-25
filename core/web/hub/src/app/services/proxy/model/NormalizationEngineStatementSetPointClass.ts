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

export class NormalizationEngineStatementSetPointClass extends models.EngineStatementFromNormalization {
    constructor() {
        super();
        this.setDiscriminator("NormalizationEngineStatementSetPointClass");
    }

    getFixupPrototypeFunction() { return NormalizationEngineStatementSetPointClass.fixupPrototype; }

    static newInstance(model: Partial<NormalizationEngineStatementSetPointClass>): NormalizationEngineStatementSetPointClass {
        let obj = Object.assign(new NormalizationEngineStatementSetPointClass(), model);
        NormalizationEngineStatementSetPointClass.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationEngineStatementSetPointClass>): NormalizationEngineStatementSetPointClass {
        if (!model) return null;
        return NormalizationEngineStatementSetPointClass.newInstance(<NormalizationEngineStatementSetPointClass> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationEngineStatementSetPointClass) {
        models.EngineStatementFromNormalization.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    pointClassId: string;

}