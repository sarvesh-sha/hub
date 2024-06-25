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

export class AlertEngineOperatorUnaryAsGroup extends models.EngineOperatorUnaryFromAlerts {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineOperatorUnaryAsGroup");
    }

    getFixupPrototypeFunction() { return AlertEngineOperatorUnaryAsGroup.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineOperatorUnaryAsGroup>): AlertEngineOperatorUnaryAsGroup {
        let obj = Object.assign(new AlertEngineOperatorUnaryAsGroup(), model);
        AlertEngineOperatorUnaryAsGroup.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineOperatorUnaryAsGroup>): AlertEngineOperatorUnaryAsGroup {
        if (!model) return null;
        return AlertEngineOperatorUnaryAsGroup.newInstance(<AlertEngineOperatorUnaryAsGroup> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineOperatorUnaryAsGroup) {
        models.EngineOperatorUnaryFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
