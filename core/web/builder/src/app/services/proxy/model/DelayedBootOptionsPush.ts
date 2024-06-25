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

export class DelayedBootOptionsPush extends models.DelayedOperation {
    constructor() {
        super();
        this.setDiscriminator("DelayedBootOptionsPush");
    }

    getFixupPrototypeFunction() { return DelayedBootOptionsPush.fixupPrototype; }

    static newInstance(model: Partial<DelayedBootOptionsPush>): DelayedBootOptionsPush {
        let obj = Object.assign(new DelayedBootOptionsPush(), model);
        DelayedBootOptionsPush.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DelayedBootOptionsPush>): DelayedBootOptionsPush {
        if (!model) return null;
        return DelayedBootOptionsPush.newInstance(<DelayedBootOptionsPush> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DelayedBootOptionsPush) {
        models.DelayedOperation.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.optValue) {
            models.BootConfigOptionAndValue.fixupPrototype(this.optValue);
        }
    }

    optValue: models.BootConfigOptionAndValue;

}