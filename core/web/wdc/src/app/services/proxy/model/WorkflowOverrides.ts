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

export class WorkflowOverrides {
    getFixupPrototypeFunction() { return WorkflowOverrides.fixupPrototype; }

    static newInstance(model: Partial<WorkflowOverrides>): WorkflowOverrides {
        let obj = Object.assign(new WorkflowOverrides(), model);
        WorkflowOverrides.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<WorkflowOverrides>): WorkflowOverrides {
        if (!model) return null;
        return WorkflowOverrides.newInstance(<WorkflowOverrides> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: WorkflowOverrides) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.WorkflowOverrides.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    workflowIds: Array<string>;

    deviceNames: { [key: string]: string; };

    deviceLocations: { [key: string]: string; };

    pointNames: { [key: string]: string; };

    pointClasses: { [key: string]: string; };

    pointParents: { [key: string]: string; };

    pointSamplingPeriods: { [key: string]: number; };

    pointSampling: { [key: string]: boolean; };

    equipmentNames: { [key: string]: string; };

    equipmentClasses: { [key: string]: string; };

    equipmentParents: { [key: string]: string; };

    equipmentLocations: { [key: string]: string; };

    equipmentMerge: { [key: string]: string; };

    removedEquipment: Array<string>;

    createdEquipment: Array<string>;

}