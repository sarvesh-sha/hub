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

export class RangeSelection {
    getFixupPrototypeFunction() { return RangeSelection.fixupPrototype; }

    static newInstance(model: Partial<RangeSelection>): RangeSelection {
        let obj = Object.assign(new RangeSelection(), model);
        RangeSelection.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<RangeSelection>): RangeSelection {
        if (!model) return null;
        return RangeSelection.newInstance(<RangeSelection> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: RangeSelection) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.RangeSelection.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (typeof this.start === "string") {
            this.start = new Date(<string><any>this.start);
        }
        if (typeof this.end === "string") {
            this.end = new Date(<string><any>this.end);
        }
    }

    range: models.TimeRangeId;

    start: Date;

    end: Date;

    zoneCreated: string;

    zone: string;

}
