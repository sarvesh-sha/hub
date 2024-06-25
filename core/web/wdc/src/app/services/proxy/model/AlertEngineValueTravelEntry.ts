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

export class AlertEngineValueTravelEntry extends models.EngineValue {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineValueTravelEntry");
    }

    getFixupPrototypeFunction() { return AlertEngineValueTravelEntry.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineValueTravelEntry>): AlertEngineValueTravelEntry {
        let obj = Object.assign(new AlertEngineValueTravelEntry(), model);
        AlertEngineValueTravelEntry.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineValueTravelEntry>): AlertEngineValueTravelEntry {
        if (!model) return null;
        return AlertEngineValueTravelEntry.newInstance(<AlertEngineValueTravelEntry> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineValueTravelEntry) {
        models.EngineValue.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.timestamp === undefined) {
            this.timestamp = 0;
        }
        if (this.longitude === undefined) {
            this.longitude = 0;
        }
        if (this.latitude === undefined) {
            this.latitude = 0;
        }
    }

    timestamp: number;

    longitude: number;

    latitude: number;

}
