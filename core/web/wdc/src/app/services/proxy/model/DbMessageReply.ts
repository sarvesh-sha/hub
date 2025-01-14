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

export class DbMessageReply {
    getFixupPrototypeFunction() { return DbMessageReply.fixupPrototype; }

    static newInstance(model: Partial<DbMessageReply>): DbMessageReply {
        let obj = Object.assign(new DbMessageReply(), model);
        DbMessageReply.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DbMessageReply>): DbMessageReply {
        if (!model) return null;
        return DbMessageReply.newInstance(<DbMessageReply> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DbMessageReply) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DbMessageReply.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

}
