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

export class EngineOperatorBinaryForStringSet extends models.EngineOperatorBinaryFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineOperatorBinaryForStringSet");
    }

    getFixupPrototypeFunction() { return EngineOperatorBinaryForStringSet.fixupPrototype; }

    static newInstance(model: Partial<EngineOperatorBinaryForStringSet>): EngineOperatorBinaryForStringSet {
        let obj = Object.assign(new EngineOperatorBinaryForStringSet(), model);
        EngineOperatorBinaryForStringSet.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineOperatorBinaryForStringSet>): EngineOperatorBinaryForStringSet {
        if (!model) return null;
        return EngineOperatorBinaryForStringSet.newInstance(<EngineOperatorBinaryForStringSet> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineOperatorBinaryForStringSet) {
        models.EngineOperatorBinaryFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    operation: models.CommonEngineSetOperation;

}
