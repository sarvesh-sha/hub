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

export class NormalizationEngineExecutionStepEquipmentClassification extends models.NormalizationEngineExecutionStep {
    constructor() {
        super();
        this.setDiscriminator("NormalizationEngineExecutionStepEquipmentClassification");
    }

    getFixupPrototypeFunction() { return NormalizationEngineExecutionStepEquipmentClassification.fixupPrototype; }

    static newInstance(model: Partial<NormalizationEngineExecutionStepEquipmentClassification>): NormalizationEngineExecutionStepEquipmentClassification {
        let obj = Object.assign(new NormalizationEngineExecutionStepEquipmentClassification(), model);
        NormalizationEngineExecutionStepEquipmentClassification.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationEngineExecutionStepEquipmentClassification>): NormalizationEngineExecutionStepEquipmentClassification {
        if (!model) return null;
        return NormalizationEngineExecutionStepEquipmentClassification.newInstance(<NormalizationEngineExecutionStepEquipmentClassification> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationEngineExecutionStepEquipmentClassification) {
        models.NormalizationEngineExecutionStep.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.equipment) {
            models.EngineValue.fixupPrototype(this.equipment);
        }
        if (this.classificationAssignment) {
            models.EquipmentClassAssignment.fixupPrototype(this.classificationAssignment);
        }
    }

    equipment: models.NormalizationEngineValueEquipment;

    classificationAssignment: models.EquipmentClassAssignment;

}
