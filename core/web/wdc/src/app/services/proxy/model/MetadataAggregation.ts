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

export class MetadataAggregation {
    getFixupPrototypeFunction() { return MetadataAggregation.fixupPrototype; }

    static newInstance(model: Partial<MetadataAggregation>): MetadataAggregation {
        let obj = Object.assign(new MetadataAggregation(), model);
        MetadataAggregation.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MetadataAggregation>): MetadataAggregation {
        if (!model) return null;
        return MetadataAggregation.newInstance(<MetadataAggregation> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MetadataAggregation) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.MetadataAggregation.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    buildingEquipments: { [key: string]: Array<string>; };

    equipmentNames: { [key: string]: string; };

    equipmentClassIds: { [key: string]: string; };

    controllerNames: { [key: string]: string; };

}
