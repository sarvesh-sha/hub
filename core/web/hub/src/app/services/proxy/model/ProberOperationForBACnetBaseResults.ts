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

export class ProberOperationForBACnetBaseResults extends models.ProberOperationBaseResults {
    getFixupPrototypeFunction() { return ProberOperationForBACnetBaseResults.fixupPrototype; }

    static newInstance(model: Partial<ProberOperationForBACnetBaseResults>): ProberOperationForBACnetBaseResults {
        let obj = Object.assign(new ProberOperationForBACnetBaseResults(), model);
        ProberOperationForBACnetBaseResults.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ProberOperationForBACnetBaseResults>): ProberOperationForBACnetBaseResults {
        if (!model) return null;
        return ProberOperationForBACnetBaseResults.newInstance(<ProberOperationForBACnetBaseResults> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ProberOperationForBACnetBaseResults) {
        models.ProberOperationBaseResults.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
