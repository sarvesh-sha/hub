/**
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Builder APIs
 * APIs and Definitions for the Optio3 Builder product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

import * as models from './models';

export class MbControlExchangeCapabilities extends models.MbControl {
    constructor() {
        super();
        this.setDiscriminator("MbControlExchangeCapabilities");
    }

    getFixupPrototypeFunction() { return MbControlExchangeCapabilities.fixupPrototype; }

    static newInstance(model: Partial<MbControlExchangeCapabilities>): MbControlExchangeCapabilities {
        let obj = Object.assign(new MbControlExchangeCapabilities(), model);
        MbControlExchangeCapabilities.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MbControlExchangeCapabilities>): MbControlExchangeCapabilities {
        if (!model) return null;
        return MbControlExchangeCapabilities.newInstance(<MbControlExchangeCapabilities> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MbControlExchangeCapabilities) {
        models.MbControl.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    available: Array<string>;

    required: Array<string>;

}
