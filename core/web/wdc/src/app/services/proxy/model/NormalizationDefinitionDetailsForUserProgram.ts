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

export class NormalizationDefinitionDetailsForUserProgram extends models.NormalizationDefinitionDetails {
    constructor() {
        super();
        this.setDiscriminator("NormalizationDefinitionDetailsForUserProgram");
    }

    getFixupPrototypeFunction() { return NormalizationDefinitionDetailsForUserProgram.fixupPrototype; }

    static newInstance(model: Partial<NormalizationDefinitionDetailsForUserProgram>): NormalizationDefinitionDetailsForUserProgram {
        let obj = Object.assign(new NormalizationDefinitionDetailsForUserProgram(), model);
        NormalizationDefinitionDetailsForUserProgram.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationDefinitionDetailsForUserProgram>): NormalizationDefinitionDetailsForUserProgram {
        if (!model) return null;
        return NormalizationDefinitionDetailsForUserProgram.newInstance(<NormalizationDefinitionDetailsForUserProgram> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationDefinitionDetailsForUserProgram) {
        models.NormalizationDefinitionDetails.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
