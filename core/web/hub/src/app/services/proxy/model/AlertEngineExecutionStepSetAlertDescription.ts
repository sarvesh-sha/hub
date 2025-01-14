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

export class AlertEngineExecutionStepSetAlertDescription extends models.AlertEngineExecutionStep {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineExecutionStepSetAlertDescription");
    }

    getFixupPrototypeFunction() { return AlertEngineExecutionStepSetAlertDescription.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineExecutionStepSetAlertDescription>): AlertEngineExecutionStepSetAlertDescription {
        let obj = Object.assign(new AlertEngineExecutionStepSetAlertDescription(), model);
        AlertEngineExecutionStepSetAlertDescription.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineExecutionStepSetAlertDescription>): AlertEngineExecutionStepSetAlertDescription {
        if (!model) return null;
        return AlertEngineExecutionStepSetAlertDescription.newInstance(<AlertEngineExecutionStepSetAlertDescription> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineExecutionStepSetAlertDescription) {
        models.AlertEngineExecutionStep.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.record) {
            models.RecordIdentity.fixupPrototype(this.record);
        }
    }

    record: models.RecordIdentity;

    description: string;

}
