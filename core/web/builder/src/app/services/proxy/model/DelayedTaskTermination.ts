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

export class DelayedTaskTermination extends models.DelayedOperation {
    constructor() {
        super();
        this.setDiscriminator("DelayedTaskTermination");
    }

    getFixupPrototypeFunction() { return DelayedTaskTermination.fixupPrototype; }

    static newInstance(model: Partial<DelayedTaskTermination>): DelayedTaskTermination {
        let obj = Object.assign(new DelayedTaskTermination(), model);
        DelayedTaskTermination.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DelayedTaskTermination>): DelayedTaskTermination {
        if (!model) return null;
        return DelayedTaskTermination.newInstance(<DelayedTaskTermination> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DelayedTaskTermination) {
        models.DelayedOperation.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.loc_task) {
            models.RecordLocator.fixupPrototype(this.loc_task);
        }
    }

    loc_task: models.RecordLocator;

    ignoreOfflineDelay: boolean;

}