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

export class NormalizationMatchHistory {
    getFixupPrototypeFunction() { return NormalizationMatchHistory.fixupPrototype; }

    static newInstance(model: Partial<NormalizationMatchHistory>): NormalizationMatchHistory {
        let obj = Object.assign(new NormalizationMatchHistory(), model);
        NormalizationMatchHistory.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationMatchHistory>): NormalizationMatchHistory {
        if (!model) return null;
        return NormalizationMatchHistory.newInstance(<NormalizationMatchHistory> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationMatchHistory) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.NormalizationMatchHistory.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.match) {
            models.NormalizationMatch.fixupPrototype(this.match);
        }
    }

    match: models.NormalizationMatch;

    before: string;

    after: string;

}
