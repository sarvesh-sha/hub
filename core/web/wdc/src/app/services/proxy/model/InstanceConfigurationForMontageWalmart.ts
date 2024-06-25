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

export class InstanceConfigurationForMontageWalmart extends models.InstanceConfigurationForTransportation {
    constructor() {
        super();
        this.setDiscriminator("InstanceConfigurationForMontageWalmart");
    }

    getFixupPrototypeFunction() { return InstanceConfigurationForMontageWalmart.fixupPrototype; }

    static newInstance(model: Partial<InstanceConfigurationForMontageWalmart>): InstanceConfigurationForMontageWalmart {
        let obj = Object.assign(new InstanceConfigurationForMontageWalmart(), model);
        InstanceConfigurationForMontageWalmart.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<InstanceConfigurationForMontageWalmart>): InstanceConfigurationForMontageWalmart {
        if (!model) return null;
        return InstanceConfigurationForMontageWalmart.newInstance(<InstanceConfigurationForMontageWalmart> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: InstanceConfigurationForMontageWalmart) {
        models.InstanceConfigurationForTransportation.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
