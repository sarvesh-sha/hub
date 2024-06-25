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

export class EngineStatementLog extends models.EngineStatementFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineStatementLog");
    }

    getFixupPrototypeFunction() { return EngineStatementLog.fixupPrototype; }

    static newInstance(model: Partial<EngineStatementLog>): EngineStatementLog {
        let obj = Object.assign(new EngineStatementLog(), model);
        EngineStatementLog.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineStatementLog>): EngineStatementLog {
        if (!model) return null;
        return EngineStatementLog.newInstance(<EngineStatementLog> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineStatementLog) {
        models.EngineStatementFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.format) {
            models.EngineBlock.fixupPrototype(this.format);
        }
        if (this.arguments) {
            for (let val of this.arguments) {
                models.EngineBlock.fixupPrototype(val);
            }
        }
    }

    format: models.EngineExpression;

    arguments: Array<models.EngineExpression>;

}
