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

export class AlertEngineStatementSetSmsSender extends models.EngineStatementFromAlerts {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineStatementSetSmsSender");
    }

    getFixupPrototypeFunction() { return AlertEngineStatementSetSmsSender.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineStatementSetSmsSender>): AlertEngineStatementSetSmsSender {
        let obj = Object.assign(new AlertEngineStatementSetSmsSender(), model);
        AlertEngineStatementSetSmsSender.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineStatementSetSmsSender>): AlertEngineStatementSetSmsSender {
        if (!model) return null;
        return AlertEngineStatementSetSmsSender.newInstance(<AlertEngineStatementSetSmsSender> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineStatementSetSmsSender) {
        models.EngineStatementFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.sms) {
            models.EngineBlock.fixupPrototype(this.sms);
        }
        if (this.sender) {
            models.EngineBlock.fixupPrototype(this.sender);
        }
    }

    sms: models.EngineExpression;

    sender: models.EngineExpression;

}
