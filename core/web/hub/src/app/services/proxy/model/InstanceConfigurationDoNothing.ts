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

export class InstanceConfigurationDoNothing extends models.InstanceConfiguration {
    constructor() {
        super();
        this.setDiscriminator("InstanceConfigurationDoNothing");
    }

    getFixupPrototypeFunction() { return InstanceConfigurationDoNothing.fixupPrototype; }

    static newInstance(model: Partial<InstanceConfigurationDoNothing>): InstanceConfigurationDoNothing {
        let obj = Object.assign(new InstanceConfigurationDoNothing(), model);
        InstanceConfigurationDoNothing.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<InstanceConfigurationDoNothing>): InstanceConfigurationDoNothing {
        if (!model) return null;
        return InstanceConfigurationDoNothing.newInstance(<InstanceConfigurationDoNothing> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: InstanceConfigurationDoNothing) {
        models.InstanceConfiguration.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
