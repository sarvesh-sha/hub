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

export class ProberObjectCANbus extends models.ProberObject {
    constructor() {
        super();
        this.setDiscriminator("ProberObjectCANbus");
    }

    getFixupPrototypeFunction() { return ProberObjectCANbus.fixupPrototype; }

    static newInstance(model: Partial<ProberObjectCANbus>): ProberObjectCANbus {
        let obj = Object.assign(new ProberObjectCANbus(), model);
        ProberObjectCANbus.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ProberObjectCANbus>): ProberObjectCANbus {
        if (!model) return null;
        return ProberObjectCANbus.newInstance(<ProberObjectCANbus> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ProberObjectCANbus) {
        models.ProberObject.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (typeof this.timestamp === "string") {
            this.timestamp = new Date(<string><any>this.timestamp);
        }
    }

    timestamp: Date;

}