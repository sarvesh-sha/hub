/**
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Builder APIs
 * APIs and Definitions for the Optio3 Builder product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

import * as models from './models';

export class DelayedBootOptionsPull extends models.DelayedOperation {
    constructor() {
        super();
        this.setDiscriminator("DelayedBootOptionsPull");
    }

    getFixupPrototypeFunction() { return DelayedBootOptionsPull.fixupPrototype; }

    static newInstance(model: Partial<DelayedBootOptionsPull>): DelayedBootOptionsPull {
        let obj = Object.assign(new DelayedBootOptionsPull(), model);
        DelayedBootOptionsPull.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DelayedBootOptionsPull>): DelayedBootOptionsPull {
        if (!model) return null;
        return DelayedBootOptionsPull.newInstance(<DelayedBootOptionsPull> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DelayedBootOptionsPull) {
        models.DelayedOperation.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
