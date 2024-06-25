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

export class DelayedImagePruning extends models.DelayedOperation {
    constructor() {
        super();
        this.setDiscriminator("DelayedImagePruning");
    }

    getFixupPrototypeFunction() { return DelayedImagePruning.fixupPrototype; }

    static newInstance(model: Partial<DelayedImagePruning>): DelayedImagePruning {
        let obj = Object.assign(new DelayedImagePruning(), model);
        DelayedImagePruning.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DelayedImagePruning>): DelayedImagePruning {
        if (!model) return null;
        return DelayedImagePruning.newInstance(<DelayedImagePruning> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DelayedImagePruning) {
        models.DelayedOperation.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.daysToKeep === undefined) {
            this.daysToKeep = 0;
        }
    }

    daysToKeep: number;

}