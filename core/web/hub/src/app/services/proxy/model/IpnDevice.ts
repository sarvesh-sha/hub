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

export class IpnDevice extends models.Device {
    constructor() {
        super();
        this.setDiscriminator("IpnDevice");
    }

    static readonly RECORD_IDENTITY: string = "IpnDevice";

    getFixupPrototypeFunction() { return IpnDevice.fixupPrototype; }

    static newInstance(model: Partial<IpnDevice>): IpnDevice {
        let obj = Object.assign(new IpnDevice(), model);
        IpnDevice.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<IpnDevice>): IpnDevice {
        if (!model) return null;
        return IpnDevice.newInstance(<IpnDevice> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: IpnDevice) {
        models.Device.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
