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

export class EquipmentClassAssignment {
    getFixupPrototypeFunction() { return EquipmentClassAssignment.fixupPrototype; }

    static newInstance(model: Partial<EquipmentClassAssignment>): EquipmentClassAssignment {
        let obj = Object.assign(new EquipmentClassAssignment(), model);
        EquipmentClassAssignment.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EquipmentClassAssignment>): EquipmentClassAssignment {
        if (!model) return null;
        return EquipmentClassAssignment.newInstance(<EquipmentClassAssignment> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EquipmentClassAssignment) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.EquipmentClassAssignment.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    regex: string;

    equipmentClassId: string;

    caseSensitive: boolean;

}
