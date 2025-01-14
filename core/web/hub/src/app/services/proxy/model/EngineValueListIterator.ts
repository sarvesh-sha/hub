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

export class EngineValueListIterator extends models.EngineValue {
    getFixupPrototypeFunction() { return EngineValueListIterator.fixupPrototype; }

    static newInstance(model: Partial<EngineValueListIterator>): EngineValueListIterator {
        let obj = Object.assign(new EngineValueListIterator(), model);
        EngineValueListIterator.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineValueListIterator>): EngineValueListIterator {
        if (!model) return null;
        return EngineValueListIterator.newInstance(<EngineValueListIterator> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineValueListIterator) {
        models.EngineValue.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
