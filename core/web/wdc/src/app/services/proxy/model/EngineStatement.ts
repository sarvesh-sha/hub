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

export class EngineStatement extends models.EngineBlock {
    getFixupPrototypeFunction() { return EngineStatement.fixupPrototype; }

    static newInstance(model: Partial<EngineStatement>): EngineStatement {
        let obj = Object.assign(new EngineStatement(), model);
        EngineStatement.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineStatement>): EngineStatement {
        if (!model) return null;
        return EngineStatement.newInstance(<EngineStatement> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineStatement) {
        models.EngineBlock.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
