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

export class AlertEngineOperatorUnaryLocationGetName extends models.EngineOperatorUnaryFromAlerts {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineOperatorUnaryLocationGetName");
    }

    getFixupPrototypeFunction() { return AlertEngineOperatorUnaryLocationGetName.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineOperatorUnaryLocationGetName>): AlertEngineOperatorUnaryLocationGetName {
        let obj = Object.assign(new AlertEngineOperatorUnaryLocationGetName(), model);
        AlertEngineOperatorUnaryLocationGetName.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineOperatorUnaryLocationGetName>): AlertEngineOperatorUnaryLocationGetName {
        if (!model) return null;
        return AlertEngineOperatorUnaryLocationGetName.newInstance(<AlertEngineOperatorUnaryLocationGetName> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineOperatorUnaryLocationGetName) {
        models.EngineOperatorUnaryFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
