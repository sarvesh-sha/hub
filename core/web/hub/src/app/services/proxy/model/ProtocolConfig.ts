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

export class ProtocolConfig {
    __type: string;

    protected setDiscriminator(type: string) {
        this.__type = type;
    }

    getFixupPrototypeFunction() { return ProtocolConfig.fixupPrototype; }

    static newInstance(model: Partial<ProtocolConfig>): ProtocolConfig {
        let obj = Object.assign(new ProtocolConfig(), model);
        ProtocolConfig.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ProtocolConfig>): ProtocolConfig {
        if (!model) return null;
        return ProtocolConfig.newInstance(<ProtocolConfig> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ProtocolConfig) {
        if (!obj) return;

        switch (obj.__type)
        {
            case "ProtocolConfigForBACnet":
                Object.setPrototypeOf(obj, models.ProtocolConfigForBACnet.prototype);
                break;
            case "ProtocolConfigForIpn":
                Object.setPrototypeOf(obj, models.ProtocolConfigForIpn.prototype);
                break;
        }

        obj.fixupFields();
    }

    fixupFields() {
    }

    samplingConfigurationId: string;

}
