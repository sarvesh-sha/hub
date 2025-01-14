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

export class LookupEntry {
    getFixupPrototypeFunction() { return LookupEntry.fixupPrototype; }

    static newInstance(model: Partial<LookupEntry>): LookupEntry {
        let obj = Object.assign(new LookupEntry(), model);
        LookupEntry.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<LookupEntry>): LookupEntry {
        if (!model) return null;
        return LookupEntry.newInstance(<LookupEntry> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: LookupEntry) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.LookupEntry.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    key: string;

    value: string;

    caseSensitive: boolean;

}
