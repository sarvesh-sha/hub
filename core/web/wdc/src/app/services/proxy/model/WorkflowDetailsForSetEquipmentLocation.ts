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

export class WorkflowDetailsForSetEquipmentLocation extends models.WorkflowDetails {
    constructor() {
        super();
        this.setDiscriminator("WorkflowDetailsForSetEquipmentLocation");
    }

    getFixupPrototypeFunction() { return WorkflowDetailsForSetEquipmentLocation.fixupPrototype; }

    static newInstance(model: Partial<WorkflowDetailsForSetEquipmentLocation>): WorkflowDetailsForSetEquipmentLocation {
        let obj = Object.assign(new WorkflowDetailsForSetEquipmentLocation(), model);
        WorkflowDetailsForSetEquipmentLocation.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<WorkflowDetailsForSetEquipmentLocation>): WorkflowDetailsForSetEquipmentLocation {
        if (!model) return null;
        return WorkflowDetailsForSetEquipmentLocation.newInstance(<WorkflowDetailsForSetEquipmentLocation> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: WorkflowDetailsForSetEquipmentLocation) {
        models.WorkflowDetails.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.equipments) {
            for (let val of this.equipments) {
                models.WorkflowAsset.fixupPrototype(val);
            }
        }
    }

    equipments: Array<models.WorkflowAsset>;

    locationSysId: string;

}