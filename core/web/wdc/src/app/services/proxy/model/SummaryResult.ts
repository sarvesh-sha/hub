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

export class SummaryResult {
    getFixupPrototypeFunction() { return SummaryResult.fixupPrototype; }

    static newInstance(model: Partial<SummaryResult>): SummaryResult {
        let obj = Object.assign(new SummaryResult(), model);
        SummaryResult.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<SummaryResult>): SummaryResult {
        if (!model) return null;
        return SummaryResult.newInstance(<SummaryResult> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: SummaryResult) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.SummaryResult.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.count === undefined) {
            this.count = 0;
        }
    }

    id: string;

    type: models.SummaryFlavor;

    label: string;

    count: number;

}