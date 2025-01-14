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

export class ProtocolConfigForIpn extends models.ProtocolConfig {
    constructor() {
        super();
        this.setDiscriminator("ProtocolConfigForIpn");
    }

    getFixupPrototypeFunction() { return ProtocolConfigForIpn.fixupPrototype; }

    static newInstance(model: Partial<ProtocolConfigForIpn>): ProtocolConfigForIpn {
        let obj = Object.assign(new ProtocolConfigForIpn(), model);
        ProtocolConfigForIpn.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ProtocolConfigForIpn>): ProtocolConfigForIpn {
        if (!model) return null;
        return ProtocolConfigForIpn.newInstance(<ProtocolConfigForIpn> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ProtocolConfigForIpn) {
        models.ProtocolConfig.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.accelerometerFrequency === undefined) {
            this.accelerometerFrequency = 0;
        }
        if (this.accelerometerRange === undefined) {
            this.accelerometerRange = 0;
        }
        if (this.accelerometerThreshold === undefined) {
            this.accelerometerThreshold = 0;
        }
        if (this.canFrequency === undefined) {
            this.canFrequency = 0;
        }
        if (this.ipnBaudrate === undefined) {
            this.ipnBaudrate = 0;
        }
        if (this.obdiiFrequency === undefined) {
            this.obdiiFrequency = 0;
        }
        if (this.i2cSensors) {
            for (let val of this.i2cSensors) {
                models.I2CSensor.fixupPrototype(val);
            }
        }
    }

    accelerometerFrequency: number;

    accelerometerRange: number;

    accelerometerThreshold: number;

    i2cSensors: Array<models.I2CSensor>;

    canPort: string;

    canFrequency: number;

    canNoTermination: boolean;

    canInvert: boolean;

    epsolarPort: string;

    epsolarInvert: boolean;

    gpsPort: string;

    holykellPort: string;

    holykellInvert: boolean;

    ipnPort: string;

    ipnBaudrate: number;

    ipnInvert: boolean;

    obdiiPort: string;

    obdiiFrequency: number;

    obdiiInvert: boolean;

    argohytosPort: string;

    stealthpowerPort: string;

    tristarPort: string;

    victronPort: string;

    montageBluetoothGatewayPort: string;

    simulate: boolean;

}
