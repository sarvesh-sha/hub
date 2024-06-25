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

export class AssetTravelLogSegment {
    getFixupPrototypeFunction() { return AssetTravelLogSegment.fixupPrototype; }

    static newInstance(model: Partial<AssetTravelLogSegment>): AssetTravelLogSegment {
        let obj = Object.assign(new AssetTravelLogSegment(), model);
        AssetTravelLogSegment.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AssetTravelLogSegment>): AssetTravelLogSegment {
        if (!model) return null;
        return AssetTravelLogSegment.newInstance(<AssetTravelLogSegment> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AssetTravelLogSegment) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.AssetTravelLogSegment.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    timestamps: Array<number>;

    longitudes: Array<number>;

    latitudes: Array<number>;

}