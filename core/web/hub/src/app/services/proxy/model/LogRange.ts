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

export class LogRange {
    getFixupPrototypeFunction() { return LogRange.fixupPrototype; }

    static newInstance(model: Partial<LogRange>): LogRange {
        let obj = Object.assign(new LogRange(), model);
        LogRange.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<LogRange>): LogRange {
        if (!model) return null;
        return LogRange.newInstance(<LogRange> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: LogRange) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.LogRange.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.startOffset === undefined) {
            this.startOffset = 0;
        }
        if (this.endOffset === undefined) {
            this.endOffset = 0;
        }
    }

    startOffset: number;

    endOffset: number;

}
