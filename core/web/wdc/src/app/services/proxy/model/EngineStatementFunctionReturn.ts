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

export class EngineStatementFunctionReturn extends models.EngineStatementFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineStatementFunctionReturn");
    }

    getFixupPrototypeFunction() { return EngineStatementFunctionReturn.fixupPrototype; }

    static newInstance(model: Partial<EngineStatementFunctionReturn>): EngineStatementFunctionReturn {
        let obj = Object.assign(new EngineStatementFunctionReturn(), model);
        EngineStatementFunctionReturn.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineStatementFunctionReturn>): EngineStatementFunctionReturn {
        if (!model) return null;
        return EngineStatementFunctionReturn.newInstance(<EngineStatementFunctionReturn> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineStatementFunctionReturn) {
        models.EngineStatementFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
