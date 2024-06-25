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

export class AlertEngineExecutionStepSetAlertStatus extends models.AlertEngineExecutionStep {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineExecutionStepSetAlertStatus");
    }

    getFixupPrototypeFunction() { return AlertEngineExecutionStepSetAlertStatus.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineExecutionStepSetAlertStatus>): AlertEngineExecutionStepSetAlertStatus {
        let obj = Object.assign(new AlertEngineExecutionStepSetAlertStatus(), model);
        AlertEngineExecutionStepSetAlertStatus.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineExecutionStepSetAlertStatus>): AlertEngineExecutionStepSetAlertStatus {
        if (!model) return null;
        return AlertEngineExecutionStepSetAlertStatus.newInstance(<AlertEngineExecutionStepSetAlertStatus> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineExecutionStepSetAlertStatus) {
        models.AlertEngineExecutionStep.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.record) {
            models.RecordIdentity.fixupPrototype(this.record);
        }
    }

    record: models.RecordIdentity;

    status: models.AlertStatus;

    statusText: string;

}