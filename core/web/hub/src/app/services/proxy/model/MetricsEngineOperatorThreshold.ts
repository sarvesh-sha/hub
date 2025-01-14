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

export class MetricsEngineOperatorThreshold extends models.EngineOperatorBinaryFromMetrics {
    constructor() {
        super();
        this.setDiscriminator("MetricsEngineOperatorThreshold");
    }

    getFixupPrototypeFunction() { return MetricsEngineOperatorThreshold.fixupPrototype; }

    static newInstance(model: Partial<MetricsEngineOperatorThreshold>): MetricsEngineOperatorThreshold {
        let obj = Object.assign(new MetricsEngineOperatorThreshold(), model);
        MetricsEngineOperatorThreshold.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MetricsEngineOperatorThreshold>): MetricsEngineOperatorThreshold {
        if (!model) return null;
        return MetricsEngineOperatorThreshold.newInstance(<MetricsEngineOperatorThreshold> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MetricsEngineOperatorThreshold) {
        models.EngineOperatorBinaryFromMetrics.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    operation: models.CommonEngineCompareOperation;

}
