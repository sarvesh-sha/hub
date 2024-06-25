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

export class NormalizationEngineStatementSetEngineeringUnits extends models.EngineStatementFromNormalization {
    constructor() {
        super();
        this.setDiscriminator("NormalizationEngineStatementSetEngineeringUnits");
    }

    getFixupPrototypeFunction() { return NormalizationEngineStatementSetEngineeringUnits.fixupPrototype; }

    static newInstance(model: Partial<NormalizationEngineStatementSetEngineeringUnits>): NormalizationEngineStatementSetEngineeringUnits {
        let obj = Object.assign(new NormalizationEngineStatementSetEngineeringUnits(), model);
        NormalizationEngineStatementSetEngineeringUnits.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationEngineStatementSetEngineeringUnits>): NormalizationEngineStatementSetEngineeringUnits {
        if (!model) return null;
        return NormalizationEngineStatementSetEngineeringUnits.newInstance(<NormalizationEngineStatementSetEngineeringUnits> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationEngineStatementSetEngineeringUnits) {
        models.EngineStatementFromNormalization.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    units: models.EngineeringUnits;

}