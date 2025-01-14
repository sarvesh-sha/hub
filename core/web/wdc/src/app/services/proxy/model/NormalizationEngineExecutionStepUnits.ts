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

export class NormalizationEngineExecutionStepUnits extends models.NormalizationEngineExecutionStep {
    constructor() {
        super();
        this.setDiscriminator("NormalizationEngineExecutionStepUnits");
    }

    getFixupPrototypeFunction() { return NormalizationEngineExecutionStepUnits.fixupPrototype; }

    static newInstance(model: Partial<NormalizationEngineExecutionStepUnits>): NormalizationEngineExecutionStepUnits {
        let obj = Object.assign(new NormalizationEngineExecutionStepUnits(), model);
        NormalizationEngineExecutionStepUnits.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationEngineExecutionStepUnits>): NormalizationEngineExecutionStepUnits {
        if (!model) return null;
        return NormalizationEngineExecutionStepUnits.newInstance(<NormalizationEngineExecutionStepUnits> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationEngineExecutionStepUnits) {
        models.NormalizationEngineExecutionStep.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    units: models.EngineeringUnits;

}
