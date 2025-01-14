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

export class AlertDefinitionVersion {
    static readonly RECORD_IDENTITY: string = "AlertDefinitionVersion";

    getFixupPrototypeFunction() { return AlertDefinitionVersion.fixupPrototype; }

    static newInstance(model: Partial<AlertDefinitionVersion>): AlertDefinitionVersion {
        let obj = Object.assign(new AlertDefinitionVersion(), model);
        AlertDefinitionVersion.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertDefinitionVersion>): AlertDefinitionVersion {
        if (!model) return null;
        return AlertDefinitionVersion.newInstance(<AlertDefinitionVersion> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertDefinitionVersion) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.AlertDefinitionVersion.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.version === undefined) {
            this.version = 0;
        }
        if (typeof this.createdOn === "string") {
            this.createdOn = new Date(<string><any>this.createdOn);
        }
        if (typeof this.updatedOn === "string") {
            this.updatedOn = new Date(<string><any>this.updatedOn);
        }
        if (this.definition) {
            models.RecordIdentity.fixupPrototype(this.definition);
        }
        if (this.details) {
            models.AlertDefinitionDetails.fixupPrototype(this.details);
        }
        if (this.predecessor) {
            models.RecordIdentity.fixupPrototype(this.predecessor);
        }
        if (this.successors) {
            for (let val of this.successors) {
                models.RecordIdentity.fixupPrototype(val);
            }
        }
    }

    sysId: string;

    createdOn: Date;

    updatedOn: Date;

    version: number;

    definition: models.RecordIdentity;

    details: models.AlertDefinitionDetails;

    predecessor: models.RecordIdentity;

    successors: Array<models.RecordIdentity>;

}
