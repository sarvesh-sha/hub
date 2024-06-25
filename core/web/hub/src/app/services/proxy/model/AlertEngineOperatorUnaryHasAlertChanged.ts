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

export class AlertEngineOperatorUnaryHasAlertChanged extends models.EngineOperatorUnaryFromAlerts {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineOperatorUnaryHasAlertChanged");
    }

    getFixupPrototypeFunction() { return AlertEngineOperatorUnaryHasAlertChanged.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineOperatorUnaryHasAlertChanged>): AlertEngineOperatorUnaryHasAlertChanged {
        let obj = Object.assign(new AlertEngineOperatorUnaryHasAlertChanged(), model);
        AlertEngineOperatorUnaryHasAlertChanged.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineOperatorUnaryHasAlertChanged>): AlertEngineOperatorUnaryHasAlertChanged {
        if (!model) return null;
        return AlertEngineOperatorUnaryHasAlertChanged.newInstance(<AlertEngineOperatorUnaryHasAlertChanged> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineOperatorUnaryHasAlertChanged) {
        models.EngineOperatorUnaryFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}