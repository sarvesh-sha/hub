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

export class CheckUsagesProgress {
    getFixupPrototypeFunction() { return CheckUsagesProgress.fixupPrototype; }

    static newInstance(model: Partial<CheckUsagesProgress>): CheckUsagesProgress {
        let obj = Object.assign(new CheckUsagesProgress(), model);
        CheckUsagesProgress.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<CheckUsagesProgress>): CheckUsagesProgress {
        if (!model) return null;
        return CheckUsagesProgress.newInstance(<CheckUsagesProgress> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: CheckUsagesProgress) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.CheckUsagesProgress.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.results) {
            models.UsageFilterResponse.fixupPrototype(this.results);
        }
    }

    status: models.BackgroundActivityStatus;

    results: models.UsageFilterResponse;

}
