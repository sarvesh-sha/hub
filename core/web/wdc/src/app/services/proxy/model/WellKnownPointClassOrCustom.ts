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

export class WellKnownPointClassOrCustom {
    getFixupPrototypeFunction() { return WellKnownPointClassOrCustom.fixupPrototype; }

    static newInstance(model: Partial<WellKnownPointClassOrCustom>): WellKnownPointClassOrCustom {
        let obj = Object.assign(new WellKnownPointClassOrCustom(), model);
        WellKnownPointClassOrCustom.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<WellKnownPointClassOrCustom>): WellKnownPointClassOrCustom {
        if (!model) return null;
        return WellKnownPointClassOrCustom.newInstance(<WellKnownPointClassOrCustom> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: WellKnownPointClassOrCustom) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.WellKnownPointClassOrCustom.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.custom === undefined) {
            this.custom = 0;
        }
    }

    known: models.WellKnownPointClass;

    custom: number;

}
