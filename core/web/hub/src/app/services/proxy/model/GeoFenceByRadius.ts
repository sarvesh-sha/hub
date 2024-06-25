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

export class GeoFenceByRadius extends models.GeoFence {
    constructor() {
        super();
        this.setDiscriminator("GeoFenceByRadius");
    }

    getFixupPrototypeFunction() { return GeoFenceByRadius.fixupPrototype; }

    static newInstance(model: Partial<GeoFenceByRadius>): GeoFenceByRadius {
        let obj = Object.assign(new GeoFenceByRadius(), model);
        GeoFenceByRadius.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<GeoFenceByRadius>): GeoFenceByRadius {
        if (!model) return null;
        return GeoFenceByRadius.newInstance(<GeoFenceByRadius> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: GeoFenceByRadius) {
        models.GeoFence.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.radius === undefined) {
            this.radius = 0;
        }
        if (this.center) {
            models.LongitudeLatitude.fixupPrototype(this.center);
        }
    }

    center: models.LongitudeLatitude;

    radius: number;

}