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

export class MetricsEngineOperatorThresholdCount extends models.EngineOperatorBinaryFromMetrics {
    constructor() {
        super();
        this.setDiscriminator("MetricsEngineOperatorThresholdCount");
    }

    getFixupPrototypeFunction() { return MetricsEngineOperatorThresholdCount.fixupPrototype; }

    static newInstance(model: Partial<MetricsEngineOperatorThresholdCount>): MetricsEngineOperatorThresholdCount {
        let obj = Object.assign(new MetricsEngineOperatorThresholdCount(), model);
        MetricsEngineOperatorThresholdCount.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MetricsEngineOperatorThresholdCount>): MetricsEngineOperatorThresholdCount {
        if (!model) return null;
        return MetricsEngineOperatorThresholdCount.newInstance(<MetricsEngineOperatorThresholdCount> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MetricsEngineOperatorThresholdCount) {
        models.EngineOperatorBinaryFromMetrics.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    operation: models.CommonEngineCompareOperation;

}
