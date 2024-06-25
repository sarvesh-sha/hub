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

export class RemoteResult {
    getFixupPrototypeFunction() { return RemoteResult.fixupPrototype; }

    static newInstance(model: Partial<RemoteResult>): RemoteResult {
        let obj = Object.assign(new RemoteResult(), model);
        RemoteResult.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<RemoteResult>): RemoteResult {
        if (!model) return null;
        return RemoteResult.newInstance(<RemoteResult> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: RemoteResult) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.RemoteResult.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.exception) {
            models.RemoteExceptionResult.fixupPrototype(this.exception);
        }
    }

    typeId: string;

    value: any;

    exception: models.RemoteExceptionResult;

}