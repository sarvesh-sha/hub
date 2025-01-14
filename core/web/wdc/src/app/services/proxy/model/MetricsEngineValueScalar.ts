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

export class MetricsEngineValueScalar extends models.MetricsEngineValue {
    constructor() {
        super();
        this.setDiscriminator("MetricsEngineValueScalar");
    }

    getFixupPrototypeFunction() { return MetricsEngineValueScalar.fixupPrototype; }

    static newInstance(model: Partial<MetricsEngineValueScalar>): MetricsEngineValueScalar {
        let obj = Object.assign(new MetricsEngineValueScalar(), model);
        MetricsEngineValueScalar.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MetricsEngineValueScalar>): MetricsEngineValueScalar {
        if (!model) return null;
        return MetricsEngineValueScalar.newInstance(<MetricsEngineValueScalar> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MetricsEngineValueScalar) {
        models.MetricsEngineValue.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.value === undefined) {
            this.value = 0;
        }
        if (this.units) {
            models.EngineeringUnitsFactors.fixupPrototype(this.units);
        }
    }

    value: number;

    units: models.EngineeringUnitsFactors;

}
