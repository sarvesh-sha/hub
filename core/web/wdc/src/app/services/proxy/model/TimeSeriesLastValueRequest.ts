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

export class TimeSeriesLastValueRequest {
    getFixupPrototypeFunction() { return TimeSeriesLastValueRequest.fixupPrototype; }

    static newInstance(model: Partial<TimeSeriesLastValueRequest>): TimeSeriesLastValueRequest {
        let obj = Object.assign(new TimeSeriesLastValueRequest(), model);
        TimeSeriesLastValueRequest.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TimeSeriesLastValueRequest>): TimeSeriesLastValueRequest {
        if (!model) return null;
        return TimeSeriesLastValueRequest.newInstance(<TimeSeriesLastValueRequest> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TimeSeriesLastValueRequest) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.TimeSeriesLastValueRequest.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.spec) {
            models.TimeSeriesPropertyRequest.fixupPrototype(this.spec);
        }
    }

    spec: models.TimeSeriesPropertyRequest;

}
