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

export class InstanceConfigurationForDigineous extends models.InstanceConfigurationWithWellKnownClasses {
    constructor() {
        super();
        this.setDiscriminator("InstanceConfigurationForDigineous");
    }

    getFixupPrototypeFunction() { return InstanceConfigurationForDigineous.fixupPrototype; }

    static newInstance(model: Partial<InstanceConfigurationForDigineous>): InstanceConfigurationForDigineous {
        let obj = Object.assign(new InstanceConfigurationForDigineous(), model);
        InstanceConfigurationForDigineous.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<InstanceConfigurationForDigineous>): InstanceConfigurationForDigineous {
        if (!model) return null;
        return InstanceConfigurationForDigineous.newInstance(<InstanceConfigurationForDigineous> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: InstanceConfigurationForDigineous) {
        models.InstanceConfigurationWithWellKnownClasses.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
