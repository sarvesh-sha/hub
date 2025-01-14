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

export class EngineLiteralStringSet extends models.EngineLiteralFromCore {
    constructor() {
        super();
        this.setDiscriminator("EngineLiteralStringSet");
    }

    getFixupPrototypeFunction() { return EngineLiteralStringSet.fixupPrototype; }

    static newInstance(model: Partial<EngineLiteralStringSet>): EngineLiteralStringSet {
        let obj = Object.assign(new EngineLiteralStringSet(), model);
        EngineLiteralStringSet.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineLiteralStringSet>): EngineLiteralStringSet {
        if (!model) return null;
        return EngineLiteralStringSet.newInstance(<EngineLiteralStringSet> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineLiteralStringSet) {
        models.EngineLiteralFromCore.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    value: Array<string>;

}
