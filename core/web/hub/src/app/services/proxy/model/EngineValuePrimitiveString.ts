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

export class EngineValuePrimitiveString extends models.EngineValuePrimitive {
    constructor() {
        super();
        this.setDiscriminator("EngineValuePrimitiveString");
    }

    getFixupPrototypeFunction() { return EngineValuePrimitiveString.fixupPrototype; }

    static newInstance(model: Partial<EngineValuePrimitiveString>): EngineValuePrimitiveString {
        let obj = Object.assign(new EngineValuePrimitiveString(), model);
        EngineValuePrimitiveString.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineValuePrimitiveString>): EngineValuePrimitiveString {
        if (!model) return null;
        return EngineValuePrimitiveString.newInstance(<EngineValuePrimitiveString> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineValuePrimitiveString) {
        models.EngineValuePrimitive.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    value: string;

}
