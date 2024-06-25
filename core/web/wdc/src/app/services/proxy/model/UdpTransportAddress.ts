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

export class UdpTransportAddress extends models.TransportAddress {
    constructor() {
        super();
        this.setDiscriminator("UdpTransportAddress");
    }

    getFixupPrototypeFunction() { return UdpTransportAddress.fixupPrototype; }

    static newInstance(model: Partial<UdpTransportAddress>): UdpTransportAddress {
        let obj = Object.assign(new UdpTransportAddress(), model);
        UdpTransportAddress.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<UdpTransportAddress>): UdpTransportAddress {
        if (!model) return null;
        return UdpTransportAddress.newInstance(<UdpTransportAddress> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: UdpTransportAddress) {
        models.TransportAddress.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.port === undefined) {
            this.port = 0;
        }
    }

    host: string;

    port: number;

}