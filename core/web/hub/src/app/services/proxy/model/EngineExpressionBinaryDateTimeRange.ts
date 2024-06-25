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

export class EngineExpressionBinaryDateTimeRange extends models.EngineOperatorBinaryFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineExpressionBinaryDateTimeRange");
    }

    getFixupPrototypeFunction() { return EngineExpressionBinaryDateTimeRange.fixupPrototype; }

    static newInstance(model: Partial<EngineExpressionBinaryDateTimeRange>): EngineExpressionBinaryDateTimeRange {
        let obj = Object.assign(new EngineExpressionBinaryDateTimeRange(), model);
        EngineExpressionBinaryDateTimeRange.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineExpressionBinaryDateTimeRange>): EngineExpressionBinaryDateTimeRange {
        if (!model) return null;
        return EngineExpressionBinaryDateTimeRange.newInstance(<EngineExpressionBinaryDateTimeRange> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineExpressionBinaryDateTimeRange) {
        models.EngineOperatorBinaryFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}