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

export class ProberOperationForBACnetToReadBBMDsResults extends models.ProberOperationForBACnetBaseResults {
    constructor() {
        super();
        this.setDiscriminator("ProberOperationForBACnetToReadBBMDsResults");
    }

    getFixupPrototypeFunction() { return ProberOperationForBACnetToReadBBMDsResults.fixupPrototype; }

    static newInstance(model: Partial<ProberOperationForBACnetToReadBBMDsResults>): ProberOperationForBACnetToReadBBMDsResults {
        let obj = Object.assign(new ProberOperationForBACnetToReadBBMDsResults(), model);
        ProberOperationForBACnetToReadBBMDsResults.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ProberOperationForBACnetToReadBBMDsResults>): ProberOperationForBACnetToReadBBMDsResults {
        if (!model) return null;
        return ProberOperationForBACnetToReadBBMDsResults.newInstance(<ProberOperationForBACnetToReadBBMDsResults> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ProberOperationForBACnetToReadBBMDsResults) {
        models.ProberOperationForBACnetBaseResults.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.bbmds) {
            for (let val of this.bbmds) {
                models.ProberBBMD.fixupPrototype(val);
            }
        }
    }

    bbmds: Array<models.ProberBBMD>;

}
