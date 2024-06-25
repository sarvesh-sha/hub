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

export class TimeSeriesRangeRequest {
    getFixupPrototypeFunction() { return TimeSeriesRangeRequest.fixupPrototype; }

    static newInstance(model: Partial<TimeSeriesRangeRequest>): TimeSeriesRangeRequest {
        let obj = Object.assign(new TimeSeriesRangeRequest(), model);
        TimeSeriesRangeRequest.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TimeSeriesRangeRequest>): TimeSeriesRangeRequest {
        if (!model) return null;
        return TimeSeriesRangeRequest.newInstance(<TimeSeriesRangeRequest> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TimeSeriesRangeRequest) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.TimeSeriesRangeRequest.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (typeof this.rangeStart === "string") {
            this.rangeStart = new Date(<string><any>this.rangeStart);
        }
        if (typeof this.rangeEnd === "string") {
            this.rangeEnd = new Date(<string><any>this.rangeEnd);
        }
        if (this.spec) {
            models.TimeSeriesPropertyRequest.fixupPrototype(this.spec);
        }
    }

    spec: models.TimeSeriesPropertyRequest;

    rangeStart: Date;

    rangeEnd: Date;

}
