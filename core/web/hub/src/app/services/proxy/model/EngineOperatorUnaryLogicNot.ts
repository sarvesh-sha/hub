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

export class EngineOperatorUnaryLogicNot extends models.EngineOperatorUnaryFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineOperatorUnaryLogicNot");
    }

    getFixupPrototypeFunction() { return EngineOperatorUnaryLogicNot.fixupPrototype; }

    static newInstance(model: Partial<EngineOperatorUnaryLogicNot>): EngineOperatorUnaryLogicNot {
        let obj = Object.assign(new EngineOperatorUnaryLogicNot(), model);
        EngineOperatorUnaryLogicNot.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineOperatorUnaryLogicNot>): EngineOperatorUnaryLogicNot {
        if (!model) return null;
        return EngineOperatorUnaryLogicNot.newInstance(<EngineOperatorUnaryLogicNot> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineOperatorUnaryLogicNot) {
        models.EngineOperatorUnaryFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}