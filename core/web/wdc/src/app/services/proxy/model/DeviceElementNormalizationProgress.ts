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

export class DeviceElementNormalizationProgress {
    getFixupPrototypeFunction() { return DeviceElementNormalizationProgress.fixupPrototype; }

    static newInstance(model: Partial<DeviceElementNormalizationProgress>): DeviceElementNormalizationProgress {
        let obj = Object.assign(new DeviceElementNormalizationProgress(), model);
        DeviceElementNormalizationProgress.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DeviceElementNormalizationProgress>): DeviceElementNormalizationProgress {
        if (!model) return null;
        return DeviceElementNormalizationProgress.newInstance(<DeviceElementNormalizationProgress> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DeviceElementNormalizationProgress) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DeviceElementNormalizationProgress.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.devicesToProcess === undefined) {
            this.devicesToProcess = 0;
        }
        if (this.devicesProcessed === undefined) {
            this.devicesProcessed = 0;
        }
        if (this.elementsProcessed === undefined) {
            this.elementsProcessed = 0;
        }
        if (this.workflowOverrides) {
            models.WorkflowOverrides.fixupPrototype(this.workflowOverrides);
        }
        if (this.details) {
            for (let val of this.details) {
                models.ClassificationPointOutput.fixupPrototype(val);
            }
        }
        if (this.equipments) {
            for (let key in this.equipments) {
                let val = this.equipments[key];
                if (val) {
                    models.NormalizationEquipment.fixupPrototype(val);
                }
            }
        }
    }

    status: models.BackgroundActivityStatus;

    devicesToProcess: number;

    devicesProcessed: number;

    elementsProcessed: number;

    allWords: { [key: string]: number; };

    allUnknownWords: { [key: string]: number; };

    equipments: { [key: string]: models.NormalizationEquipment; };

    equipmentRelationships: { [key: string]: Array<string>; };

    details: Array<models.ClassificationPointOutput>;

    workflowOverrides: models.WorkflowOverrides;

}
