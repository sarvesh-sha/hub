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

export class EngineValuePrimitiveBoolean extends models.EngineValuePrimitive {
    constructor() {
        super();
        this.setDiscriminator("EngineValuePrimitiveBoolean");
    }

    getFixupPrototypeFunction() { return EngineValuePrimitiveBoolean.fixupPrototype; }

    static newInstance(model: Partial<EngineValuePrimitiveBoolean>): EngineValuePrimitiveBoolean {
        let obj = Object.assign(new EngineValuePrimitiveBoolean(), model);
        EngineValuePrimitiveBoolean.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineValuePrimitiveBoolean>): EngineValuePrimitiveBoolean {
        if (!model) return null;
        return EngineValuePrimitiveBoolean.newInstance(<EngineValuePrimitiveBoolean> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineValuePrimitiveBoolean) {
        models.EngineValuePrimitive.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    value: boolean;

}