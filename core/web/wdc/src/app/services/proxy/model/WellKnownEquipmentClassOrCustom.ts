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

export class WellKnownEquipmentClassOrCustom {
    getFixupPrototypeFunction() { return WellKnownEquipmentClassOrCustom.fixupPrototype; }

    static newInstance(model: Partial<WellKnownEquipmentClassOrCustom>): WellKnownEquipmentClassOrCustom {
        let obj = Object.assign(new WellKnownEquipmentClassOrCustom(), model);
        WellKnownEquipmentClassOrCustom.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<WellKnownEquipmentClassOrCustom>): WellKnownEquipmentClassOrCustom {
        if (!model) return null;
        return WellKnownEquipmentClassOrCustom.newInstance(<WellKnownEquipmentClassOrCustom> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: WellKnownEquipmentClassOrCustom) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.WellKnownEquipmentClassOrCustom.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.custom === undefined) {
            this.custom = 0;
        }
    }

    known: models.WellKnownEquipmentClass;

    custom: number;

}