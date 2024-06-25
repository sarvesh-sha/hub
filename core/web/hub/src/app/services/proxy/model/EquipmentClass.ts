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

export class EquipmentClass {
    getFixupPrototypeFunction() { return EquipmentClass.fixupPrototype; }

    static newInstance(model: Partial<EquipmentClass>): EquipmentClass {
        let obj = Object.assign(new EquipmentClass(), model);
        EquipmentClass.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EquipmentClass>): EquipmentClass {
        if (!model) return null;
        return EquipmentClass.newInstance(<EquipmentClass> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EquipmentClass) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.EquipmentClass.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.id === undefined) {
            this.id = 0;
        }
    }

    id: number;

    equipClassName: string;

    description: string;

    azureDigitalTwin: string;

    wellKnown: models.WellKnownEquipmentClass;

    tags: Array<string>;

}