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

export class AssetFilterResponse {
    getFixupPrototypeFunction() { return AssetFilterResponse.fixupPrototype; }

    static newInstance(model: Partial<AssetFilterResponse>): AssetFilterResponse {
        let obj = Object.assign(new AssetFilterResponse(), model);
        AssetFilterResponse.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AssetFilterResponse>): AssetFilterResponse {
        if (!model) return null;
        return AssetFilterResponse.newInstance(<AssetFilterResponse> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AssetFilterResponse) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.AssetFilterResponse.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.version === undefined) {
            this.version = 0;
        }
        if (this.offset === undefined) {
            this.offset = 0;
        }
        if (this.nextOffset === undefined) {
            this.nextOffset = 0;
        }
        if (this.results) {
            for (let val of this.results) {
                models.RecordIdentity.fixupPrototype(val);
            }
        }
    }

    version: number;

    offset: number;

    nextOffset: number;

    results: Array<models.RecordIdentity>;

}
