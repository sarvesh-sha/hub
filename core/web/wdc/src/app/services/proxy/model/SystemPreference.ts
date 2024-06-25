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

export class SystemPreference {
    static readonly RECORD_IDENTITY: string = "SystemPreference";

    getFixupPrototypeFunction() { return SystemPreference.fixupPrototype; }

    static newInstance(model: Partial<SystemPreference>): SystemPreference {
        let obj = Object.assign(new SystemPreference(), model);
        SystemPreference.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<SystemPreference>): SystemPreference {
        if (!model) return null;
        return SystemPreference.newInstance(<SystemPreference> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: SystemPreference) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.SystemPreference.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (typeof this.createdOn === "string") {
            this.createdOn = new Date(<string><any>this.createdOn);
        }
        if (typeof this.updatedOn === "string") {
            this.updatedOn = new Date(<string><any>this.updatedOn);
        }
    }

    sysId: string;

    createdOn: Date;

    updatedOn: Date;

    key: string;

    value: string;

}
