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

export class AlertEngineOperatorUnaryAssetGetLocation extends models.EngineOperatorUnaryFromAlerts {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineOperatorUnaryAssetGetLocation");
    }

    getFixupPrototypeFunction() { return AlertEngineOperatorUnaryAssetGetLocation.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineOperatorUnaryAssetGetLocation>): AlertEngineOperatorUnaryAssetGetLocation {
        let obj = Object.assign(new AlertEngineOperatorUnaryAssetGetLocation(), model);
        AlertEngineOperatorUnaryAssetGetLocation.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineOperatorUnaryAssetGetLocation>): AlertEngineOperatorUnaryAssetGetLocation {
        if (!model) return null;
        return AlertEngineOperatorUnaryAssetGetLocation.newInstance(<AlertEngineOperatorUnaryAssetGetLocation> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineOperatorUnaryAssetGetLocation) {
        models.EngineOperatorUnaryFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
