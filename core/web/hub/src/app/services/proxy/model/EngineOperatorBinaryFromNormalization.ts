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

export class EngineOperatorBinaryFromNormalization extends models.EngineOperatorBinary {
    getFixupPrototypeFunction() { return EngineOperatorBinaryFromNormalization.fixupPrototype; }

    static newInstance(model: Partial<EngineOperatorBinaryFromNormalization>): EngineOperatorBinaryFromNormalization {
        let obj = Object.assign(new EngineOperatorBinaryFromNormalization(), model);
        EngineOperatorBinaryFromNormalization.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineOperatorBinaryFromNormalization>): EngineOperatorBinaryFromNormalization {
        if (!model) return null;
        return EngineOperatorBinaryFromNormalization.newInstance(<EngineOperatorBinaryFromNormalization> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineOperatorBinaryFromNormalization) {
        models.EngineOperatorBinary.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
