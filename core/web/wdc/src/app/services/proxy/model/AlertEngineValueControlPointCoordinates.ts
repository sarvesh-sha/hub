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

export class AlertEngineValueControlPointCoordinates extends models.EngineValue {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineValueControlPointCoordinates");
    }

    getFixupPrototypeFunction() { return AlertEngineValueControlPointCoordinates.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineValueControlPointCoordinates>): AlertEngineValueControlPointCoordinates {
        let obj = Object.assign(new AlertEngineValueControlPointCoordinates(), model);
        AlertEngineValueControlPointCoordinates.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineValueControlPointCoordinates>): AlertEngineValueControlPointCoordinates {
        if (!model) return null;
        return AlertEngineValueControlPointCoordinates.newInstance(<AlertEngineValueControlPointCoordinates> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineValueControlPointCoordinates) {
        models.EngineValue.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.longitude) {
            models.RecordIdentity.fixupPrototype(this.longitude);
        }
        if (this.latitude) {
            models.RecordIdentity.fixupPrototype(this.latitude);
        }
    }

    longitude: models.RecordIdentity;

    latitude: models.RecordIdentity;

}
