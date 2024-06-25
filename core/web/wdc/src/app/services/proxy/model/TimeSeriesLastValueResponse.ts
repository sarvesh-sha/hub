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

export class TimeSeriesLastValueResponse {
    getFixupPrototypeFunction() { return TimeSeriesLastValueResponse.fixupPrototype; }

    static newInstance(model: Partial<TimeSeriesLastValueResponse>): TimeSeriesLastValueResponse {
        let obj = Object.assign(new TimeSeriesLastValueResponse(), model);
        TimeSeriesLastValueResponse.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TimeSeriesLastValueResponse>): TimeSeriesLastValueResponse {
        if (!model) return null;
        return TimeSeriesLastValueResponse.newInstance(<TimeSeriesLastValueResponse> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TimeSeriesLastValueResponse) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.TimeSeriesLastValueResponse.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (typeof this.timestamp === "string") {
            this.timestamp = new Date(<string><any>this.timestamp);
        }
    }

    timestamp: Date;

    value: any;

}
