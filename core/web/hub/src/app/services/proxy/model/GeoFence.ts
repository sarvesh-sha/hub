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

export class GeoFence {
    __type: string;

    protected setDiscriminator(type: string) {
        this.__type = type;
    }

    getFixupPrototypeFunction() { return GeoFence.fixupPrototype; }

    static newInstance(model: Partial<GeoFence>): GeoFence {
        let obj = Object.assign(new GeoFence(), model);
        GeoFence.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<GeoFence>): GeoFence {
        if (!model) return null;
        return GeoFence.newInstance(<GeoFence> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: GeoFence) {
        if (!obj) return;

        switch (obj.__type)
        {
            case "GeoFenceByPolygon":
                Object.setPrototypeOf(obj, models.GeoFenceByPolygon.prototype);
                break;
            case "GeoFenceByRadius":
                Object.setPrototypeOf(obj, models.GeoFenceByRadius.prototype);
                break;
        }

        obj.fixupFields();
    }

    fixupFields() {
    }

    uniqueId: string;

}