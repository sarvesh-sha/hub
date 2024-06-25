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

export class AlertDefinition {
    static readonly RECORD_IDENTITY: string = "AlertDefinition";

    getFixupPrototypeFunction() { return AlertDefinition.fixupPrototype; }

    static newInstance(model: Partial<AlertDefinition>): AlertDefinition {
        let obj = Object.assign(new AlertDefinition(), model);
        AlertDefinition.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertDefinition>): AlertDefinition {
        if (!model) return null;
        return AlertDefinition.newInstance(<AlertDefinition> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertDefinition) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.AlertDefinition.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.lastOffset === undefined) {
            this.lastOffset = 0;
        }
        if (typeof this.createdOn === "string") {
            this.createdOn = new Date(<string><any>this.createdOn);
        }
        if (typeof this.updatedOn === "string") {
            this.updatedOn = new Date(<string><any>this.updatedOn);
        }
        if (typeof this.lastOutput === "string") {
            this.lastOutput = new Date(<string><any>this.lastOutput);
        }
        if (this.headVersion) {
            models.RecordIdentity.fixupPrototype(this.headVersion);
        }
        if (this.releaseVersion) {
            models.RecordIdentity.fixupPrototype(this.releaseVersion);
        }
        if (this.versions) {
            for (let val of this.versions) {
                models.RecordIdentity.fixupPrototype(val);
            }
        }
    }

    sysId: string;

    createdOn: Date;

    updatedOn: Date;

    title: string;

    description: string;

    active: boolean;

    purpose: models.AlertDefinitionPurpose;

    lastOutput: Date;

    lastOffset: number;

    headVersion: models.RecordIdentity;

    releaseVersion: models.RecordIdentity;

    versions: Array<models.RecordIdentity>;

}
