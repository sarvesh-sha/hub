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

export class MetricsEngineOperatorGpsDistance extends models.EngineExpressionFromMetrics {
    constructor() {
        super();
        this.setDiscriminator("MetricsEngineOperatorGpsDistance");
    }

    getFixupPrototypeFunction() { return MetricsEngineOperatorGpsDistance.fixupPrototype; }

    static newInstance(model: Partial<MetricsEngineOperatorGpsDistance>): MetricsEngineOperatorGpsDistance {
        let obj = Object.assign(new MetricsEngineOperatorGpsDistance(), model);
        MetricsEngineOperatorGpsDistance.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MetricsEngineOperatorGpsDistance>): MetricsEngineOperatorGpsDistance {
        if (!model) return null;
        return MetricsEngineOperatorGpsDistance.newInstance(<MetricsEngineOperatorGpsDistance> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MetricsEngineOperatorGpsDistance) {
        models.EngineExpressionFromMetrics.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.latitude) {
            models.EngineBlock.fixupPrototype(this.latitude);
        }
        if (this.longitude) {
            models.EngineBlock.fixupPrototype(this.longitude);
        }
        if (this.speed) {
            models.EngineBlock.fixupPrototype(this.speed);
        }
    }

    latitude: models.EngineExpression;

    longitude: models.EngineExpression;

    speed: models.EngineExpression;

}