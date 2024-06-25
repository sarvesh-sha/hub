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

export class EngineStatementProcedureCall extends models.EngineStatementFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineStatementProcedureCall");
    }

    getFixupPrototypeFunction() { return EngineStatementProcedureCall.fixupPrototype; }

    static newInstance(model: Partial<EngineStatementProcedureCall>): EngineStatementProcedureCall {
        let obj = Object.assign(new EngineStatementProcedureCall(), model);
        EngineStatementProcedureCall.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineStatementProcedureCall>): EngineStatementProcedureCall {
        if (!model) return null;
        return EngineStatementProcedureCall.newInstance(<EngineStatementProcedureCall> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineStatementProcedureCall) {
        models.EngineStatementFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.arguments) {
            for (let val of this.arguments) {
                models.EngineBlock.fixupPrototype(val);
            }
        }
    }

    functionId: string;

    arguments: Array<models.EngineVariableAssignment>;

}
