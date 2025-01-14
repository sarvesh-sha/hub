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

export class LocationClass {
    getFixupPrototypeFunction() { return LocationClass.fixupPrototype; }

    static newInstance(model: Partial<LocationClass>): LocationClass {
        let obj = Object.assign(new LocationClass(), model);
        LocationClass.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<LocationClass>): LocationClass {
        if (!model) return null;
        return LocationClass.newInstance(<LocationClass> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: LocationClass) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.LocationClass.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    id: models.LocationType;

    description: string;

    azureDigitalTwin: string;

    tags: Array<string>;

}
