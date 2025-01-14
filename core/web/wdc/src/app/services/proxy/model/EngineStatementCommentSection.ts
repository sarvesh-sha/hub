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

export class EngineStatementCommentSection extends models.EngineStatementFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineStatementCommentSection");
    }

    getFixupPrototypeFunction() { return EngineStatementCommentSection.fixupPrototype; }

    static newInstance(model: Partial<EngineStatementCommentSection>): EngineStatementCommentSection {
        let obj = Object.assign(new EngineStatementCommentSection(), model);
        EngineStatementCommentSection.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineStatementCommentSection>): EngineStatementCommentSection {
        if (!model) return null;
        return EngineStatementCommentSection.newInstance(<EngineStatementCommentSection> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineStatementCommentSection) {
        models.EngineStatementFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.statements) {
            for (let val of this.statements) {
                models.EngineBlock.fixupPrototype(val);
            }
        }
    }

    text: string;

    statements: Array<models.EngineStatement>;

}
