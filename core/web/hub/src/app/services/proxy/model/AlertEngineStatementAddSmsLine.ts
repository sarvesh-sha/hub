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

export class AlertEngineStatementAddSmsLine extends models.EngineStatementFromAlerts {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineStatementAddSmsLine");
    }

    getFixupPrototypeFunction() { return AlertEngineStatementAddSmsLine.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineStatementAddSmsLine>): AlertEngineStatementAddSmsLine {
        let obj = Object.assign(new AlertEngineStatementAddSmsLine(), model);
        AlertEngineStatementAddSmsLine.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineStatementAddSmsLine>): AlertEngineStatementAddSmsLine {
        if (!model) return null;
        return AlertEngineStatementAddSmsLine.newInstance(<AlertEngineStatementAddSmsLine> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineStatementAddSmsLine) {
        models.EngineStatementFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.sms) {
            models.EngineBlock.fixupPrototype(this.sms);
        }
        if (this.text) {
            models.EngineBlock.fixupPrototype(this.text);
        }
    }

    sms: models.EngineExpression;

    text: models.EngineExpression;

}