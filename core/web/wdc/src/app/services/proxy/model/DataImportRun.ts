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

export class DataImportRun {
    getFixupPrototypeFunction() { return DataImportRun.fixupPrototype; }

    static newInstance(model: Partial<DataImportRun>): DataImportRun {
        let obj = Object.assign(new DataImportRun(), model);
        DataImportRun.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DataImportRun>): DataImportRun {
        if (!model) return null;
        return DataImportRun.newInstance(<DataImportRun> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DataImportRun) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DataImportRun.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.devices) {
            for (let val of this.devices) {
                models.RecordIdentity.fixupPrototype(val);
            }
        }
    }

    dataImportsId: string;

    devices: Array<models.RecordIdentity>;

}
