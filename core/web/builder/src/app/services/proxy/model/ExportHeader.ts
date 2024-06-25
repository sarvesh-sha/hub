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

export class ExportHeader {
    getFixupPrototypeFunction() { return ExportHeader.fixupPrototype; }

    static newInstance(model: Partial<ExportHeader>): ExportHeader {
        let obj = Object.assign(new ExportHeader(), model);
        ExportHeader.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ExportHeader>): ExportHeader {
        if (!model) return null;
        return ExportHeader.newInstance(<ExportHeader> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ExportHeader) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.ExportHeader.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.columns) {
            for (let val of this.columns) {
                models.ExportColumn.fixupPrototype(val);
            }
        }
    }

    sheetName: string;

    columns: Array<models.ExportColumn>;

}
