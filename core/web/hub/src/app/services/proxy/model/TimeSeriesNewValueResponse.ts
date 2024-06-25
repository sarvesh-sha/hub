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

export class TimeSeriesNewValueResponse {
    getFixupPrototypeFunction() { return TimeSeriesNewValueResponse.fixupPrototype; }

    static newInstance(model: Partial<TimeSeriesNewValueResponse>): TimeSeriesNewValueResponse {
        let obj = Object.assign(new TimeSeriesNewValueResponse(), model);
        TimeSeriesNewValueResponse.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TimeSeriesNewValueResponse>): TimeSeriesNewValueResponse {
        if (!model) return null;
        return TimeSeriesNewValueResponse.newInstance(<TimeSeriesNewValueResponse> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TimeSeriesNewValueResponse) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.TimeSeriesNewValueResponse.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.accepted === undefined) {
            this.accepted = 0;
        }
        if (this.rejected === undefined) {
            this.rejected = 0;
        }
    }

    accepted: number;

    rejected: number;

}