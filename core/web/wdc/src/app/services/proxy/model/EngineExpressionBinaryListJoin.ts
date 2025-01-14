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

export class EngineExpressionBinaryListJoin extends models.EngineOperatorBinaryFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineExpressionBinaryListJoin");
    }

    getFixupPrototypeFunction() { return EngineExpressionBinaryListJoin.fixupPrototype; }

    static newInstance(model: Partial<EngineExpressionBinaryListJoin>): EngineExpressionBinaryListJoin {
        let obj = Object.assign(new EngineExpressionBinaryListJoin(), model);
        EngineExpressionBinaryListJoin.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineExpressionBinaryListJoin>): EngineExpressionBinaryListJoin {
        if (!model) return null;
        return EngineExpressionBinaryListJoin.newInstance(<EngineExpressionBinaryListJoin> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineExpressionBinaryListJoin) {
        models.EngineOperatorBinaryFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
