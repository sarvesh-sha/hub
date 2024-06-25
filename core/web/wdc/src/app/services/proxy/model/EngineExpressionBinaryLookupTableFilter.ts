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

export class EngineExpressionBinaryLookupTableFilter extends models.EngineOperatorBinaryFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineExpressionBinaryLookupTableFilter");
    }

    getFixupPrototypeFunction() { return EngineExpressionBinaryLookupTableFilter.fixupPrototype; }

    static newInstance(model: Partial<EngineExpressionBinaryLookupTableFilter>): EngineExpressionBinaryLookupTableFilter {
        let obj = Object.assign(new EngineExpressionBinaryLookupTableFilter(), model);
        EngineExpressionBinaryLookupTableFilter.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineExpressionBinaryLookupTableFilter>): EngineExpressionBinaryLookupTableFilter {
        if (!model) return null;
        return EngineExpressionBinaryLookupTableFilter.newInstance(<EngineExpressionBinaryLookupTableFilter> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineExpressionBinaryLookupTableFilter) {
        models.EngineOperatorBinaryFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
