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

export class MetricsEngineOperatorVectorBinaryAdd extends models.MetricsEngineOperatorVectorBinary {
    constructor() {
        super();
        this.setDiscriminator("MetricsEngineOperatorVectorBinaryAdd");
    }

    getFixupPrototypeFunction() { return MetricsEngineOperatorVectorBinaryAdd.fixupPrototype; }

    static newInstance(model: Partial<MetricsEngineOperatorVectorBinaryAdd>): MetricsEngineOperatorVectorBinaryAdd {
        let obj = Object.assign(new MetricsEngineOperatorVectorBinaryAdd(), model);
        MetricsEngineOperatorVectorBinaryAdd.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MetricsEngineOperatorVectorBinaryAdd>): MetricsEngineOperatorVectorBinaryAdd {
        if (!model) return null;
        return MetricsEngineOperatorVectorBinaryAdd.newInstance(<MetricsEngineOperatorVectorBinaryAdd> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MetricsEngineOperatorVectorBinaryAdd) {
        models.MetricsEngineOperatorVectorBinary.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
