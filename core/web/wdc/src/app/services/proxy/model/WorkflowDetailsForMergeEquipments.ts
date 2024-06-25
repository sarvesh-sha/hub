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

export class WorkflowDetailsForMergeEquipments extends models.WorkflowDetails {
    constructor() {
        super();
        this.setDiscriminator("WorkflowDetailsForMergeEquipments");
    }

    getFixupPrototypeFunction() { return WorkflowDetailsForMergeEquipments.fixupPrototype; }

    static newInstance(model: Partial<WorkflowDetailsForMergeEquipments>): WorkflowDetailsForMergeEquipments {
        let obj = Object.assign(new WorkflowDetailsForMergeEquipments(), model);
        WorkflowDetailsForMergeEquipments.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<WorkflowDetailsForMergeEquipments>): WorkflowDetailsForMergeEquipments {
        if (!model) return null;
        return WorkflowDetailsForMergeEquipments.newInstance(<WorkflowDetailsForMergeEquipments> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: WorkflowDetailsForMergeEquipments) {
        models.WorkflowDetails.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.equipment1) {
            models.WorkflowAsset.fixupPrototype(this.equipment1);
        }
        if (this.equipment2) {
            models.WorkflowAsset.fixupPrototype(this.equipment2);
        }
    }

    equipment1: models.WorkflowAsset;

    equipment2: models.WorkflowAsset;

}