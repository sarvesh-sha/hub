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

export class InstanceConfigurationForMerlinSolar extends models.InstanceConfigurationForTransportation {
    constructor() {
        super();
        this.setDiscriminator("InstanceConfigurationForMerlinSolar");
    }

    getFixupPrototypeFunction() { return InstanceConfigurationForMerlinSolar.fixupPrototype; }

    static newInstance(model: Partial<InstanceConfigurationForMerlinSolar>): InstanceConfigurationForMerlinSolar {
        let obj = Object.assign(new InstanceConfigurationForMerlinSolar(), model);
        InstanceConfigurationForMerlinSolar.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<InstanceConfigurationForMerlinSolar>): InstanceConfigurationForMerlinSolar {
        if (!model) return null;
        return InstanceConfigurationForMerlinSolar.newInstance(<InstanceConfigurationForMerlinSolar> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: InstanceConfigurationForMerlinSolar) {
        models.InstanceConfigurationForTransportation.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}