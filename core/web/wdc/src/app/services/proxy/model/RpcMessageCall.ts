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

export class RpcMessageCall extends models.RpcMessage {
    constructor() {
        super();
        this.setDiscriminator("RpcMessageCall");
    }

    getFixupPrototypeFunction() { return RpcMessageCall.fixupPrototype; }

    static newInstance(model: Partial<RpcMessageCall>): RpcMessageCall {
        let obj = Object.assign(new RpcMessageCall(), model);
        RpcMessageCall.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<RpcMessageCall>): RpcMessageCall {
        if (!model) return null;
        return RpcMessageCall.newInstance(<RpcMessageCall> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: RpcMessageCall) {
        models.RpcMessage.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.timeout === undefined) {
            this.timeout = 0;
        }
        if (this.descriptor) {
            models.RemoteCallDescriptor.fixupPrototype(this.descriptor);
        }
    }

    instanceId: string;

    callId: string;

    descriptor: models.RemoteCallDescriptor;

    timeout: number;

    timeoutUnit: models.TimeUnit;

}
