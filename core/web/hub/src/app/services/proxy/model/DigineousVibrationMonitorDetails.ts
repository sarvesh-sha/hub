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

export class DigineousVibrationMonitorDetails {
    getFixupPrototypeFunction() { return DigineousVibrationMonitorDetails.fixupPrototype; }

    static newInstance(model: Partial<DigineousVibrationMonitorDetails>): DigineousVibrationMonitorDetails {
        let obj = Object.assign(new DigineousVibrationMonitorDetails(), model);
        DigineousVibrationMonitorDetails.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DigineousVibrationMonitorDetails>): DigineousVibrationMonitorDetails {
        if (!model) return null;
        return DigineousVibrationMonitorDetails.newInstance(<DigineousVibrationMonitorDetails> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DigineousVibrationMonitorDetails) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DigineousVibrationMonitorDetails.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.id === undefined) {
            this.id = 0;
        }
        if (this.plantId === undefined) {
            this.plantId = 0;
        }
    }

    id: number;

    plantId: number;

    label: string;

    deviceName: string;

}