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

export class TimeSeriesSinglePropertyResponse {
    getFixupPrototypeFunction() { return TimeSeriesSinglePropertyResponse.fixupPrototype; }

    static newInstance(model: Partial<TimeSeriesSinglePropertyResponse>): TimeSeriesSinglePropertyResponse {
        let obj = Object.assign(new TimeSeriesSinglePropertyResponse(), model);
        TimeSeriesSinglePropertyResponse.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TimeSeriesSinglePropertyResponse>): TimeSeriesSinglePropertyResponse {
        if (!model) return null;
        return TimeSeriesSinglePropertyResponse.newInstance(<TimeSeriesSinglePropertyResponse> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TimeSeriesSinglePropertyResponse) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.TimeSeriesSinglePropertyResponse.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.results) {
            models.TimeSeriesPropertyResponse.fixupPrototype(this.results);
        }
    }

    timestamps: Array<number>;

    results: models.TimeSeriesPropertyResponse;

    deltaEncoded: boolean;

}
