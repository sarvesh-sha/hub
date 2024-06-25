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

export class MbControlReply extends models.MessageBusPayload {
    constructor() {
        super();
        this.setDiscriminator("MbControlReply");
    }

    getFixupPrototypeFunction() { return MbControlReply.fixupPrototype; }

    static newInstance(model: Partial<MbControlReply>): MbControlReply {
        let obj = Object.assign(new MbControlReply(), model);
        MbControlReply.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MbControlReply>): MbControlReply {
        if (!model) return null;
        return MbControlReply.newInstance(<MbControlReply> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MbControlReply) {
        models.MessageBusPayload.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}