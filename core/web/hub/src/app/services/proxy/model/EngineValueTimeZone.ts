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

export class EngineValueTimeZone extends models.EngineValue {
    constructor() {
        super();
        this.setDiscriminator("EngineValueTimeZone");
    }

    getFixupPrototypeFunction() { return EngineValueTimeZone.fixupPrototype; }

    static newInstance(model: Partial<EngineValueTimeZone>): EngineValueTimeZone {
        let obj = Object.assign(new EngineValueTimeZone(), model);
        EngineValueTimeZone.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineValueTimeZone>): EngineValueTimeZone {
        if (!model) return null;
        return EngineValueTimeZone.newInstance(<EngineValueTimeZone> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineValueTimeZone) {
        models.EngineValue.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    value: string;

}
