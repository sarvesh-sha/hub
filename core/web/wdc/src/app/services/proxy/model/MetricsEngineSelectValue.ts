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

export class MetricsEngineSelectValue extends models.EngineValue {
    constructor() {
        super();
        this.setDiscriminator("MetricsEngineSelectValue");
    }

    getFixupPrototypeFunction() { return MetricsEngineSelectValue.fixupPrototype; }

    static newInstance(model: Partial<MetricsEngineSelectValue>): MetricsEngineSelectValue {
        let obj = Object.assign(new MetricsEngineSelectValue(), model);
        MetricsEngineSelectValue.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MetricsEngineSelectValue>): MetricsEngineSelectValue {
        if (!model) return null;
        return MetricsEngineSelectValue.newInstance(<MetricsEngineSelectValue> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MetricsEngineSelectValue) {
        models.EngineValue.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.values) {
            models.TimeSeriesPropertyResponse.fixupPrototype(this.values);
        }
    }

    identifier: string;

    values: models.TimeSeriesPropertyResponse;

}
