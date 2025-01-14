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

export class EngineOperatorUnaryStringToNumber extends models.EngineOperatorUnaryFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineOperatorUnaryStringToNumber");
    }

    getFixupPrototypeFunction() { return EngineOperatorUnaryStringToNumber.fixupPrototype; }

    static newInstance(model: Partial<EngineOperatorUnaryStringToNumber>): EngineOperatorUnaryStringToNumber {
        let obj = Object.assign(new EngineOperatorUnaryStringToNumber(), model);
        EngineOperatorUnaryStringToNumber.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineOperatorUnaryStringToNumber>): EngineOperatorUnaryStringToNumber {
        if (!model) return null;
        return EngineOperatorUnaryStringToNumber.newInstance(<EngineOperatorUnaryStringToNumber> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineOperatorUnaryStringToNumber) {
        models.EngineOperatorUnaryFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
