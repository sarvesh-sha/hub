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

export class RestPerfDescriptor extends models.BaseAssetDescriptor {
    constructor() {
        super();
        this.setDiscriminator("RestPerfDescriptor");
    }

    getFixupPrototypeFunction() { return RestPerfDescriptor.fixupPrototype; }

    static newInstance(model: Partial<RestPerfDescriptor>): RestPerfDescriptor {
        let obj = Object.assign(new RestPerfDescriptor(), model);
        RestPerfDescriptor.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<RestPerfDescriptor>): RestPerfDescriptor {
        if (!model) return null;
        return RestPerfDescriptor.newInstance(<RestPerfDescriptor> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: RestPerfDescriptor) {
        models.BaseAssetDescriptor.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    path: string;

}
