/**
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Builder APIs
 * APIs and Definitions for the Optio3 Builder product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

import * as models from './models';

export class JobSource {
    static readonly RECORD_IDENTITY: string = "JobSource";

    getFixupPrototypeFunction() { return JobSource.fixupPrototype; }

    static newInstance(model: Partial<JobSource>): JobSource {
        let obj = Object.assign(new JobSource(), model);
        JobSource.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<JobSource>): JobSource {
        if (!model) return null;
        return JobSource.newInstance(<JobSource> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: JobSource) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.JobSource.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (typeof this.createdOn === "string") {
            this.createdOn = new Date(<string><any>this.createdOn);
        }
        if (typeof this.updatedOn === "string") {
            this.updatedOn = new Date(<string><any>this.updatedOn);
        }
        if (this.owningJob) {
            models.RecordIdentity.fixupPrototype(this.owningJob);
        }
        if (this.repo) {
            models.RecordIdentity.fixupPrototype(this.repo);
        }
    }

    sysId: string;

    createdOn: Date;

    updatedOn: Date;

    owningJob: models.RecordIdentity;

    repo: models.RecordIdentity;

    branch: string;

    commit: string;

}
