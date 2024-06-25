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

export class EngineOperatorUnaryDateTimeRangeFromCurrentTime extends models.EngineOperatorUnaryFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineOperatorUnaryDateTimeRangeFromCurrentTime");
    }

    getFixupPrototypeFunction() { return EngineOperatorUnaryDateTimeRangeFromCurrentTime.fixupPrototype; }

    static newInstance(model: Partial<EngineOperatorUnaryDateTimeRangeFromCurrentTime>): EngineOperatorUnaryDateTimeRangeFromCurrentTime {
        let obj = Object.assign(new EngineOperatorUnaryDateTimeRangeFromCurrentTime(), model);
        EngineOperatorUnaryDateTimeRangeFromCurrentTime.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineOperatorUnaryDateTimeRangeFromCurrentTime>): EngineOperatorUnaryDateTimeRangeFromCurrentTime {
        if (!model) return null;
        return EngineOperatorUnaryDateTimeRangeFromCurrentTime.newInstance(<EngineOperatorUnaryDateTimeRangeFromCurrentTime> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineOperatorUnaryDateTimeRangeFromCurrentTime) {
        models.EngineOperatorUnaryFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
