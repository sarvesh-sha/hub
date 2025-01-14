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

export class AlertEngineStatementSetControlPointMetadata extends models.EngineStatementFromAlerts {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineStatementSetControlPointMetadata");
    }

    getFixupPrototypeFunction() { return AlertEngineStatementSetControlPointMetadata.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineStatementSetControlPointMetadata>): AlertEngineStatementSetControlPointMetadata {
        let obj = Object.assign(new AlertEngineStatementSetControlPointMetadata(), model);
        AlertEngineStatementSetControlPointMetadata.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineStatementSetControlPointMetadata>): AlertEngineStatementSetControlPointMetadata {
        if (!model) return null;
        return AlertEngineStatementSetControlPointMetadata.newInstance(<AlertEngineStatementSetControlPointMetadata> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineStatementSetControlPointMetadata) {
        models.EngineStatementFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.controlPoint) {
            models.EngineBlock.fixupPrototype(this.controlPoint);
        }
        if (this.value) {
            models.EngineBlock.fixupPrototype(this.value);
        }
    }

    controlPoint: models.EngineExpression;

    key: string;

    value: models.EngineExpression;

}
