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

export class LocationPolygon {
    getFixupPrototypeFunction() { return LocationPolygon.fixupPrototype; }

    static newInstance(model: Partial<LocationPolygon>): LocationPolygon {
        let obj = Object.assign(new LocationPolygon(), model);
        LocationPolygon.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<LocationPolygon>): LocationPolygon {
        if (!model) return null;
        return LocationPolygon.newInstance(<LocationPolygon> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: LocationPolygon) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.LocationPolygon.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.points) {
            for (let val of this.points) {
                models.LongitudeLatitude.fixupPrototype(val);
            }
        }
    }

    points: Array<models.LongitudeLatitude>;

}