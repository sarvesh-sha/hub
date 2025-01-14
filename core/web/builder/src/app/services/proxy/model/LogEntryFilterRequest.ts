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

export class LogEntryFilterRequest {
    getFixupPrototypeFunction() { return LogEntryFilterRequest.fixupPrototype; }

    static newInstance(model: Partial<LogEntryFilterRequest>): LogEntryFilterRequest {
        let obj = Object.assign(new LogEntryFilterRequest(), model);
        LogEntryFilterRequest.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<LogEntryFilterRequest>): LogEntryFilterRequest {
        if (!model) return null;
        return LogEntryFilterRequest.newInstance(<LogEntryFilterRequest> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: LogEntryFilterRequest) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.LogEntryFilterRequest.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.fromOffset === undefined) {
            this.fromOffset = 0;
        }
        if (this.toOffset === undefined) {
            this.toOffset = 0;
        }
        if (this.limit === undefined) {
            this.limit = 0;
        }
    }

    fromOffset: number;

    toOffset: number;

    limit: number;

    levels: Array<string>;

    threads: Array<string>;

    hosts: Array<string>;

    selectors: Array<string>;

    filter: string;

}
