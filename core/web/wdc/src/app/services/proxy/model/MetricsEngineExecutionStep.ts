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

export class MetricsEngineExecutionStep extends models.EngineExecutionStep {
    constructor() {
        super();
        this.setDiscriminator("MetricsEngineExecutionStep");
    }

    getFixupPrototypeFunction() { return MetricsEngineExecutionStep.fixupPrototype; }

    static newInstance(model: Partial<MetricsEngineExecutionStep>): MetricsEngineExecutionStep {
        let obj = Object.assign(new MetricsEngineExecutionStep(), model);
        MetricsEngineExecutionStep.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MetricsEngineExecutionStep>): MetricsEngineExecutionStep {
        if (!model) return null;
        return MetricsEngineExecutionStep.newInstance(<MetricsEngineExecutionStep> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MetricsEngineExecutionStep) {
        models.EngineExecutionStep.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
