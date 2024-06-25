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

export class ReportDefinitionVersion {
    static readonly RECORD_IDENTITY: string = "ReportDefinitionVersion";

    getFixupPrototypeFunction() { return ReportDefinitionVersion.fixupPrototype; }

    static newInstance(model: Partial<ReportDefinitionVersion>): ReportDefinitionVersion {
        let obj = Object.assign(new ReportDefinitionVersion(), model);
        ReportDefinitionVersion.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ReportDefinitionVersion>): ReportDefinitionVersion {
        if (!model) return null;
        return ReportDefinitionVersion.newInstance(<ReportDefinitionVersion> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ReportDefinitionVersion) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.ReportDefinitionVersion.prototype);

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
            models.ReportDefinitionDetails.fixupPrototype(this.details);
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

    details: models.ReportDefinitionDetails;

    predecessor: models.RecordIdentity;

    successors: Array<models.RecordIdentity>;

}
