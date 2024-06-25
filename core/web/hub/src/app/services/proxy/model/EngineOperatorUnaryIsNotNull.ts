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

export class EngineOperatorUnaryIsNotNull extends models.EngineOperatorUnaryFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineOperatorUnaryIsNotNull");
    }

    getFixupPrototypeFunction() { return EngineOperatorUnaryIsNotNull.fixupPrototype; }

    static newInstance(model: Partial<EngineOperatorUnaryIsNotNull>): EngineOperatorUnaryIsNotNull {
        let obj = Object.assign(new EngineOperatorUnaryIsNotNull(), model);
        EngineOperatorUnaryIsNotNull.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineOperatorUnaryIsNotNull>): EngineOperatorUnaryIsNotNull {
        if (!model) return null;
        return EngineOperatorUnaryIsNotNull.newInstance(<EngineOperatorUnaryIsNotNull> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineOperatorUnaryIsNotNull) {
        models.EngineOperatorUnaryFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
