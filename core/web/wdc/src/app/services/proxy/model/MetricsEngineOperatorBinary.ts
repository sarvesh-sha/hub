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

export class MetricsEngineOperatorBinary extends models.EngineOperatorBinaryFromMetrics {
    constructor() {
        super();
        this.setDiscriminator("MetricsEngineOperatorBinary");
    }

    getFixupPrototypeFunction() { return MetricsEngineOperatorBinary.fixupPrototype; }

    static newInstance(model: Partial<MetricsEngineOperatorBinary>): MetricsEngineOperatorBinary {
        let obj = Object.assign(new MetricsEngineOperatorBinary(), model);
        MetricsEngineOperatorBinary.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MetricsEngineOperatorBinary>): MetricsEngineOperatorBinary {
        if (!model) return null;
        return MetricsEngineOperatorBinary.newInstance(<MetricsEngineOperatorBinary> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MetricsEngineOperatorBinary) {
        models.EngineOperatorBinaryFromMetrics.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    operation: models.CommonEngineArithmeticOperation;

}
