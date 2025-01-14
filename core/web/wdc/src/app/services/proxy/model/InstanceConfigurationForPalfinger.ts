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

export class InstanceConfigurationForPalfinger extends models.InstanceConfigurationForTransportation {
    constructor() {
        super();
        this.setDiscriminator("InstanceConfigurationForPalfinger");
    }

    getFixupPrototypeFunction() { return InstanceConfigurationForPalfinger.fixupPrototype; }

    static newInstance(model: Partial<InstanceConfigurationForPalfinger>): InstanceConfigurationForPalfinger {
        let obj = Object.assign(new InstanceConfigurationForPalfinger(), model);
        InstanceConfigurationForPalfinger.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<InstanceConfigurationForPalfinger>): InstanceConfigurationForPalfinger {
        if (!model) return null;
        return InstanceConfigurationForPalfinger.newInstance(<InstanceConfigurationForPalfinger> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: InstanceConfigurationForPalfinger) {
        models.InstanceConfigurationForTransportation.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
