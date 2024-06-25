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

export class AlertEngineOperatorUnaryAssetQueryNot extends models.EngineOperatorUnaryFromAlerts {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineOperatorUnaryAssetQueryNot");
    }

    getFixupPrototypeFunction() { return AlertEngineOperatorUnaryAssetQueryNot.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineOperatorUnaryAssetQueryNot>): AlertEngineOperatorUnaryAssetQueryNot {
        let obj = Object.assign(new AlertEngineOperatorUnaryAssetQueryNot(), model);
        AlertEngineOperatorUnaryAssetQueryNot.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineOperatorUnaryAssetQueryNot>): AlertEngineOperatorUnaryAssetQueryNot {
        if (!model) return null;
        return AlertEngineOperatorUnaryAssetQueryNot.newInstance(<AlertEngineOperatorUnaryAssetQueryNot> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineOperatorUnaryAssetQueryNot) {
        models.EngineOperatorUnaryFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}