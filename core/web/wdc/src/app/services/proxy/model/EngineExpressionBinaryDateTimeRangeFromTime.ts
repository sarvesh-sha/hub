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

export class EngineExpressionBinaryDateTimeRangeFromTime extends models.EngineOperatorBinaryFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineExpressionBinaryDateTimeRangeFromTime");
    }

    getFixupPrototypeFunction() { return EngineExpressionBinaryDateTimeRangeFromTime.fixupPrototype; }

    static newInstance(model: Partial<EngineExpressionBinaryDateTimeRangeFromTime>): EngineExpressionBinaryDateTimeRangeFromTime {
        let obj = Object.assign(new EngineExpressionBinaryDateTimeRangeFromTime(), model);
        EngineExpressionBinaryDateTimeRangeFromTime.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineExpressionBinaryDateTimeRangeFromTime>): EngineExpressionBinaryDateTimeRangeFromTime {
        if (!model) return null;
        return EngineExpressionBinaryDateTimeRangeFromTime.newInstance(<EngineExpressionBinaryDateTimeRangeFromTime> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineExpressionBinaryDateTimeRangeFromTime) {
        models.EngineOperatorBinaryFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
