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

export class InstanceConfigurationWithWellKnownClasses extends models.InstanceConfiguration {
    getFixupPrototypeFunction() { return InstanceConfigurationWithWellKnownClasses.fixupPrototype; }

    static newInstance(model: Partial<InstanceConfigurationWithWellKnownClasses>): InstanceConfigurationWithWellKnownClasses {
        let obj = Object.assign(new InstanceConfigurationWithWellKnownClasses(), model);
        InstanceConfigurationWithWellKnownClasses.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<InstanceConfigurationWithWellKnownClasses>): InstanceConfigurationWithWellKnownClasses {
        if (!model) return null;
        return InstanceConfigurationWithWellKnownClasses.newInstance(<InstanceConfigurationWithWellKnownClasses> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: InstanceConfigurationWithWellKnownClasses) {
        models.InstanceConfiguration.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
