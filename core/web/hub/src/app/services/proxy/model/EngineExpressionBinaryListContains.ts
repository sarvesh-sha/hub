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

export class EngineExpressionBinaryListContains extends models.EngineOperatorBinaryFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineExpressionBinaryListContains");
    }

    getFixupPrototypeFunction() { return EngineExpressionBinaryListContains.fixupPrototype; }

    static newInstance(model: Partial<EngineExpressionBinaryListContains>): EngineExpressionBinaryListContains {
        let obj = Object.assign(new EngineExpressionBinaryListContains(), model);
        EngineExpressionBinaryListContains.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineExpressionBinaryListContains>): EngineExpressionBinaryListContains {
        if (!model) return null;
        return EngineExpressionBinaryListContains.newInstance(<EngineExpressionBinaryListContains> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineExpressionBinaryListContains) {
        models.EngineOperatorBinaryFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
