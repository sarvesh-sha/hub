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

export class WorkflowDetailsForNewEquipment extends models.WorkflowDetails {
    constructor() {
        super();
        this.setDiscriminator("WorkflowDetailsForNewEquipment");
    }

    getFixupPrototypeFunction() { return WorkflowDetailsForNewEquipment.fixupPrototype; }

    static newInstance(model: Partial<WorkflowDetailsForNewEquipment>): WorkflowDetailsForNewEquipment {
        let obj = Object.assign(new WorkflowDetailsForNewEquipment(), model);
        WorkflowDetailsForNewEquipment.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<WorkflowDetailsForNewEquipment>): WorkflowDetailsForNewEquipment {
        if (!model) return null;
        return WorkflowDetailsForNewEquipment.newInstance(<WorkflowDetailsForNewEquipment> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: WorkflowDetailsForNewEquipment) {
        models.WorkflowDetails.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.parentEquipment) {
            models.WorkflowAsset.fixupPrototype(this.parentEquipment);
        }
    }

    equipmentKey: string;

    equipmentName: string;

    equipmentClassId: string;

    locationSysId: string;

    parentEquipment: models.WorkflowAsset;

}
