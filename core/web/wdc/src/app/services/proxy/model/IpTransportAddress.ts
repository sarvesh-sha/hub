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

export class IpTransportAddress extends models.TransportAddress {
    constructor() {
        super();
        this.setDiscriminator("IpTransportAddress");
    }

    getFixupPrototypeFunction() { return IpTransportAddress.fixupPrototype; }

    static newInstance(model: Partial<IpTransportAddress>): IpTransportAddress {
        let obj = Object.assign(new IpTransportAddress(), model);
        IpTransportAddress.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<IpTransportAddress>): IpTransportAddress {
        if (!model) return null;
        return IpTransportAddress.newInstance(<IpTransportAddress> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: IpTransportAddress) {
        models.TransportAddress.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    host: string;

}
