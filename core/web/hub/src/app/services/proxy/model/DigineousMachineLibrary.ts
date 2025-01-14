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

export class DigineousMachineLibrary {
    getFixupPrototypeFunction() { return DigineousMachineLibrary.fixupPrototype; }

    static newInstance(model: Partial<DigineousMachineLibrary>): DigineousMachineLibrary {
        let obj = Object.assign(new DigineousMachineLibrary(), model);
        DigineousMachineLibrary.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DigineousMachineLibrary>): DigineousMachineLibrary {
        if (!model) return null;
        return DigineousMachineLibrary.newInstance(<DigineousMachineLibrary> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DigineousMachineLibrary) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DigineousMachineLibrary.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.equipmentClass) {
            models.WellKnownEquipmentClassOrCustom.fixupPrototype(this.equipmentClass);
        }
    }

    id: string;

    name: string;

    equipmentClass: models.WellKnownEquipmentClassOrCustom;

    deviceTemplates: Array<string>;

}
