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

export class AlertEngineOperatorBinaryForDeliveryOptions extends models.EngineOperatorBinaryFromAlerts {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineOperatorBinaryForDeliveryOptions");
    }

    getFixupPrototypeFunction() { return AlertEngineOperatorBinaryForDeliveryOptions.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineOperatorBinaryForDeliveryOptions>): AlertEngineOperatorBinaryForDeliveryOptions {
        let obj = Object.assign(new AlertEngineOperatorBinaryForDeliveryOptions(), model);
        AlertEngineOperatorBinaryForDeliveryOptions.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineOperatorBinaryForDeliveryOptions>): AlertEngineOperatorBinaryForDeliveryOptions {
        if (!model) return null;
        return AlertEngineOperatorBinaryForDeliveryOptions.newInstance(<AlertEngineOperatorBinaryForDeliveryOptions> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineOperatorBinaryForDeliveryOptions) {
        models.EngineOperatorBinaryFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    operation: models.CommonEngineSetOperation;

}
