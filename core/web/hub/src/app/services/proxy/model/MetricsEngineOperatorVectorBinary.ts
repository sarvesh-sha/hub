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

export class MetricsEngineOperatorVectorBinary extends models.EngineOperatorBinaryFromMetrics {
    getFixupPrototypeFunction() { return MetricsEngineOperatorVectorBinary.fixupPrototype; }

    static newInstance(model: Partial<MetricsEngineOperatorVectorBinary>): MetricsEngineOperatorVectorBinary {
        let obj = Object.assign(new MetricsEngineOperatorVectorBinary(), model);
        MetricsEngineOperatorVectorBinary.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MetricsEngineOperatorVectorBinary>): MetricsEngineOperatorVectorBinary {
        if (!model) return null;
        return MetricsEngineOperatorVectorBinary.newInstance(<MetricsEngineOperatorVectorBinary> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MetricsEngineOperatorVectorBinary) {
        models.EngineOperatorBinaryFromMetrics.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}