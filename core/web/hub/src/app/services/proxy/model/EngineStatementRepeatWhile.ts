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

export class EngineStatementRepeatWhile extends models.EngineStatementFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineStatementRepeatWhile");
    }

    getFixupPrototypeFunction() { return EngineStatementRepeatWhile.fixupPrototype; }

    static newInstance(model: Partial<EngineStatementRepeatWhile>): EngineStatementRepeatWhile {
        let obj = Object.assign(new EngineStatementRepeatWhile(), model);
        EngineStatementRepeatWhile.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineStatementRepeatWhile>): EngineStatementRepeatWhile {
        if (!model) return null;
        return EngineStatementRepeatWhile.newInstance(<EngineStatementRepeatWhile> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineStatementRepeatWhile) {
        models.EngineStatementFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.condition) {
            models.EngineBlock.fixupPrototype(this.condition);
        }
        if (this.statements) {
            for (let val of this.statements) {
                models.EngineBlock.fixupPrototype(val);
            }
        }
    }

    condition: models.EngineExpression;

    statements: Array<models.EngineStatement>;

}
