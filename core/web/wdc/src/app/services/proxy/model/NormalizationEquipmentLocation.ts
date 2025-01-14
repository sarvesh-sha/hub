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

export class NormalizationEquipmentLocation {
    getFixupPrototypeFunction() { return NormalizationEquipmentLocation.fixupPrototype; }

    static newInstance(model: Partial<NormalizationEquipmentLocation>): NormalizationEquipmentLocation {
        let obj = Object.assign(new NormalizationEquipmentLocation(), model);
        NormalizationEquipmentLocation.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationEquipmentLocation>): NormalizationEquipmentLocation {
        if (!model) return null;
        return NormalizationEquipmentLocation.newInstance(<NormalizationEquipmentLocation> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationEquipmentLocation) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.NormalizationEquipmentLocation.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    name: string;

    type: models.LocationType;

}
