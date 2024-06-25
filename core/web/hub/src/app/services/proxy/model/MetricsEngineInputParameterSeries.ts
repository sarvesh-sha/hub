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

export class MetricsEngineInputParameterSeries extends models.EngineInputParameterFromMetrics {
    constructor() {
        super();
        this.setDiscriminator("MetricsEngineInputParameterSeries");
    }

    getFixupPrototypeFunction() { return MetricsEngineInputParameterSeries.fixupPrototype; }

    static newInstance(model: Partial<MetricsEngineInputParameterSeries>): MetricsEngineInputParameterSeries {
        let obj = Object.assign(new MetricsEngineInputParameterSeries(), model);
        MetricsEngineInputParameterSeries.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MetricsEngineInputParameterSeries>): MetricsEngineInputParameterSeries {
        if (!model) return null;
        return MetricsEngineInputParameterSeries.newInstance(<MetricsEngineInputParameterSeries> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MetricsEngineInputParameterSeries) {
        models.EngineInputParameterFromMetrics.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    nodeId: string;

}
