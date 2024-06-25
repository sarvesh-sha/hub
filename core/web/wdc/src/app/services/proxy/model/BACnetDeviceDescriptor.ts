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

export class BACnetDeviceDescriptor extends models.BaseAssetDescriptor {
    constructor() {
        super();
        this.setDiscriminator("BACnetDeviceDescriptor");
    }

    getFixupPrototypeFunction() { return BACnetDeviceDescriptor.fixupPrototype; }

    static newInstance(model: Partial<BACnetDeviceDescriptor>): BACnetDeviceDescriptor {
        let obj = Object.assign(new BACnetDeviceDescriptor(), model);
        BACnetDeviceDescriptor.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<BACnetDeviceDescriptor>): BACnetDeviceDescriptor {
        if (!model) return null;
        return BACnetDeviceDescriptor.newInstance(<BACnetDeviceDescriptor> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: BACnetDeviceDescriptor) {
        models.BaseAssetDescriptor.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.maxAdpu === undefined) {
            this.maxAdpu = 0;
        }
        if (this.address) {
            models.BACnetDeviceAddress.fixupPrototype(this.address);
        }
        if (this.bacnetAddress) {
            models.BACnetAddress.fixupPrototype(this.bacnetAddress);
        }
        if (this.transport) {
            models.TransportAddress.fixupPrototype(this.transport);
        }
    }

    address: models.BACnetDeviceAddress;

    bacnetAddress: models.BACnetAddress;

    transport: models.TransportAddress;

    segmentation: models.BACnetSegmentation;

    maxAdpu: number;

}