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

export class WorkflowHistory {
    static readonly RECORD_IDENTITY: string = "WorkflowHistory";

    getFixupPrototypeFunction() { return WorkflowHistory.fixupPrototype; }

    static newInstance(model: Partial<WorkflowHistory>): WorkflowHistory {
        let obj = Object.assign(new WorkflowHistory(), model);
        WorkflowHistory.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<WorkflowHistory>): WorkflowHistory {
        if (!model) return null;
        return WorkflowHistory.newInstance(<WorkflowHistory> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: WorkflowHistory) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.WorkflowHistory.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (typeof this.createdOn === "string") {
            this.createdOn = new Date(<string><any>this.createdOn);
        }
        if (typeof this.updatedOn === "string") {
            this.updatedOn = new Date(<string><any>this.updatedOn);
        }
        if (this.workflow) {
            models.RecordIdentity.fixupPrototype(this.workflow);
        }
        if (this.user) {
            models.RecordIdentity.fixupPrototype(this.user);
        }
    }

    sysId: string;

    createdOn: Date;

    updatedOn: Date;

    workflow: models.RecordIdentity;

    type: models.WorkflowEventType;

    text: string;

    user: models.RecordIdentity;

}
