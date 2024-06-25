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

export class WorkflowDetailsForSetDeviceLocation extends models.WorkflowDetails {
    constructor() {
        super();
        this.setDiscriminator("WorkflowDetailsForSetDeviceLocation");
    }

    getFixupPrototypeFunction() { return WorkflowDetailsForSetDeviceLocation.fixupPrototype; }

    static newInstance(model: Partial<WorkflowDetailsForSetDeviceLocation>): WorkflowDetailsForSetDeviceLocation {
        let obj = Object.assign(new WorkflowDetailsForSetDeviceLocation(), model);
        WorkflowDetailsForSetDeviceLocation.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<WorkflowDetailsForSetDeviceLocation>): WorkflowDetailsForSetDeviceLocation {
        if (!model) return null;
        return WorkflowDetailsForSetDeviceLocation.newInstance(<WorkflowDetailsForSetDeviceLocation> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: WorkflowDetailsForSetDeviceLocation) {
        models.WorkflowDetails.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.devices) {
            for (let val of this.devices) {
                models.WorkflowAsset.fixupPrototype(val);
            }
        }
    }

    devices: Array<models.WorkflowAsset>;

    locationSysId: string;

}