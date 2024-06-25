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

export class Location extends models.Asset {
    constructor() {
        super();
        this.setDiscriminator("Location");
    }

    static readonly RECORD_IDENTITY: string = "Location";

    getFixupPrototypeFunction() { return Location.fixupPrototype; }

    static newInstance(model: Partial<Location>): Location {
        let obj = Object.assign(new Location(), model);
        Location.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<Location>): Location {
        if (!model) return null;
        return Location.newInstance(<Location> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: Location) {
        models.Asset.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.geo) {
            models.LongitudeLatitude.fixupPrototype(this.geo);
        }
        if (this.fences) {
            for (let val of this.fences) {
                models.GeoFence.fixupPrototype(val);
            }
        }
    }

    type: models.LocationType;

    phone: string;

    address: string;

    timeZone: string;

    geo: models.LongitudeLatitude;

    fences: Array<models.GeoFence>;

}