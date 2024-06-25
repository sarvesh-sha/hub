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

export class DelayedImagePull extends models.DelayedOperation {
    constructor() {
        super();
        this.setDiscriminator("DelayedImagePull");
    }

    getFixupPrototypeFunction() { return DelayedImagePull.fixupPrototype; }

    static newInstance(model: Partial<DelayedImagePull>): DelayedImagePull {
        let obj = Object.assign(new DelayedImagePull(), model);
        DelayedImagePull.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DelayedImagePull>): DelayedImagePull {
        if (!model) return null;
        return DelayedImagePull.newInstance(<DelayedImagePull> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DelayedImagePull) {
        models.DelayedOperation.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.loc_image) {
            models.RecordLocator.fixupPrototype(this.loc_image);
        }
    }

    loc_image: models.RecordLocator;

    targetTag: string;

}