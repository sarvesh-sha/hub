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

export class TimeSeriesSinglePropertyRequest {
    getFixupPrototypeFunction() { return TimeSeriesSinglePropertyRequest.fixupPrototype; }

    static newInstance(model: Partial<TimeSeriesSinglePropertyRequest>): TimeSeriesSinglePropertyRequest {
        let obj = Object.assign(new TimeSeriesSinglePropertyRequest(), model);
        TimeSeriesSinglePropertyRequest.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TimeSeriesSinglePropertyRequest>): TimeSeriesSinglePropertyRequest {
        if (!model) return null;
        return TimeSeriesSinglePropertyRequest.newInstance(<TimeSeriesSinglePropertyRequest> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TimeSeriesSinglePropertyRequest) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.TimeSeriesSinglePropertyRequest.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.maxSamples === undefined) {
            this.maxSamples = 0;
        }
        if (this.maxGapBetweenSamples === undefined) {
            this.maxGapBetweenSamples = 0;
        }
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

    maxSamples: number;

    maxGapBetweenSamples: number;

    skipMissing: boolean;

    rangeStart: Date;

    rangeEnd: Date;

    spec: models.TimeSeriesPropertyRequest;

    deltaEncode: boolean;

}