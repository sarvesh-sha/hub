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

export class EngineInputParameterDateTime extends models.EngineInputParameterFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineInputParameterDateTime");
    }

    getFixupPrototypeFunction() { return EngineInputParameterDateTime.fixupPrototype; }

    static newInstance(model: Partial<EngineInputParameterDateTime>): EngineInputParameterDateTime {
        let obj = Object.assign(new EngineInputParameterDateTime(), model);
        EngineInputParameterDateTime.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineInputParameterDateTime>): EngineInputParameterDateTime {
        if (!model) return null;
        return EngineInputParameterDateTime.newInstance(<EngineInputParameterDateTime> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineInputParameterDateTime) {
        models.EngineInputParameterFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (typeof this.value === "string") {
            this.value = new Date(<string><any>this.value);
        }
    }

    value: Date;

}