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

export class MetadataAggregationPoint {
    getFixupPrototypeFunction() { return MetadataAggregationPoint.fixupPrototype; }

    static newInstance(model: Partial<MetadataAggregationPoint>): MetadataAggregationPoint {
        let obj = Object.assign(new MetadataAggregationPoint(), model);
        MetadataAggregationPoint.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MetadataAggregationPoint>): MetadataAggregationPoint {
        if (!model) return null;
        return MetadataAggregationPoint.newInstance(<MetadataAggregationPoint> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MetadataAggregationPoint) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.MetadataAggregationPoint.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    pointId: string;

    pointName: string;

    pointNameRaw: string;

    pointNameBackup: string;

    identifier: string;

    pointClassId: string;

    buildingId: string;

    locationSysId: string;

    equipmentId: string;

    tags: Array<string>;

}