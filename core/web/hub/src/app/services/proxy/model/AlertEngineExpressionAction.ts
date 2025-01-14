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

export class AlertEngineExpressionAction extends models.EngineExpressionFromAlerts {
    getFixupPrototypeFunction() { return AlertEngineExpressionAction.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineExpressionAction>): AlertEngineExpressionAction {
        let obj = Object.assign(new AlertEngineExpressionAction(), model);
        AlertEngineExpressionAction.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineExpressionAction>): AlertEngineExpressionAction {
        if (!model) return null;
        return AlertEngineExpressionAction.newInstance(<AlertEngineExpressionAction> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineExpressionAction) {
        models.EngineExpressionFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.alert) {
            models.EngineBlock.fixupPrototype(this.alert);
        }
        if (this.deliveryOptions) {
            models.EngineBlock.fixupPrototype(this.deliveryOptions);
        }
    }

    alert: models.EngineExpression;

    deliveryOptions: models.EngineExpression;

}
