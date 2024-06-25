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

export class AlertEngineOperatorBinaryAssetQueryAnd extends models.EngineOperatorBinaryFromAlerts {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineOperatorBinaryAssetQueryAnd");
    }

    getFixupPrototypeFunction() { return AlertEngineOperatorBinaryAssetQueryAnd.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineOperatorBinaryAssetQueryAnd>): AlertEngineOperatorBinaryAssetQueryAnd {
        let obj = Object.assign(new AlertEngineOperatorBinaryAssetQueryAnd(), model);
        AlertEngineOperatorBinaryAssetQueryAnd.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineOperatorBinaryAssetQueryAnd>): AlertEngineOperatorBinaryAssetQueryAnd {
        if (!model) return null;
        return AlertEngineOperatorBinaryAssetQueryAnd.newInstance(<AlertEngineOperatorBinaryAssetQueryAnd> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineOperatorBinaryAssetQueryAnd) {
        models.EngineOperatorBinaryFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}