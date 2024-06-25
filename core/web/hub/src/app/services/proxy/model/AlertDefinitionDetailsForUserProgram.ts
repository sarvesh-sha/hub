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

export class AlertDefinitionDetailsForUserProgram extends models.AlertDefinitionDetails {
    constructor() {
        super();
        this.setDiscriminator("AlertDefinitionDetailsForUserProgram");
    }

    getFixupPrototypeFunction() { return AlertDefinitionDetailsForUserProgram.fixupPrototype; }

    static newInstance(model: Partial<AlertDefinitionDetailsForUserProgram>): AlertDefinitionDetailsForUserProgram {
        let obj = Object.assign(new AlertDefinitionDetailsForUserProgram(), model);
        AlertDefinitionDetailsForUserProgram.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertDefinitionDetailsForUserProgram>): AlertDefinitionDetailsForUserProgram {
        if (!model) return null;
        return AlertDefinitionDetailsForUserProgram.newInstance(<AlertDefinitionDetailsForUserProgram> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertDefinitionDetailsForUserProgram) {
        models.AlertDefinitionDetails.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
