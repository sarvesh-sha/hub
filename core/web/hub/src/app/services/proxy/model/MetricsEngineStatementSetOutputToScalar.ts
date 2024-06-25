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

export class MetricsEngineStatementSetOutputToScalar extends models.EngineStatementFromMetrics {
    constructor() {
        super();
        this.setDiscriminator("MetricsEngineStatementSetOutputToScalar");
    }

    getFixupPrototypeFunction() { return MetricsEngineStatementSetOutputToScalar.fixupPrototype; }

    static newInstance(model: Partial<MetricsEngineStatementSetOutputToScalar>): MetricsEngineStatementSetOutputToScalar {
        let obj = Object.assign(new MetricsEngineStatementSetOutputToScalar(), model);
        MetricsEngineStatementSetOutputToScalar.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MetricsEngineStatementSetOutputToScalar>): MetricsEngineStatementSetOutputToScalar {
        if (!model) return null;
        return MetricsEngineStatementSetOutputToScalar.newInstance(<MetricsEngineStatementSetOutputToScalar> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MetricsEngineStatementSetOutputToScalar) {
        models.EngineStatementFromMetrics.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.scalar) {
            models.EngineBlock.fixupPrototype(this.scalar);
        }
    }

    scalar: models.EngineExpression;

}
