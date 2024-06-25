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

export class MbControlJoinChannel extends models.MbControl {
    constructor() {
        super();
        this.setDiscriminator("MbControlJoinChannel");
    }

    getFixupPrototypeFunction() { return MbControlJoinChannel.fixupPrototype; }

    static newInstance(model: Partial<MbControlJoinChannel>): MbControlJoinChannel {
        let obj = Object.assign(new MbControlJoinChannel(), model);
        MbControlJoinChannel.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MbControlJoinChannel>): MbControlJoinChannel {
        if (!model) return null;
        return MbControlJoinChannel.newInstance(<MbControlJoinChannel> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MbControlJoinChannel) {
        models.MbControl.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    channel: string;

}