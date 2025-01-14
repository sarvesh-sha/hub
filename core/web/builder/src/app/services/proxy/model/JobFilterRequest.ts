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

export class JobFilterRequest {
    getFixupPrototypeFunction() { return JobFilterRequest.fixupPrototype; }

    static newInstance(model: Partial<JobFilterRequest>): JobFilterRequest {
        let obj = Object.assign(new JobFilterRequest(), model);
        JobFilterRequest.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<JobFilterRequest>): JobFilterRequest {
        if (!model) return null;
        return JobFilterRequest.newInstance(<JobFilterRequest> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: JobFilterRequest) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.JobFilterRequest.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (typeof this.after === "string") {
            this.after = new Date(<string><any>this.after);
        }
        if (typeof this.before === "string") {
            this.before = new Date(<string><any>this.before);
        }
        if (this.sortBy) {
            for (let val of this.sortBy) {
                models.SortCriteria.fixupPrototype(val);
            }
        }
    }

    after: Date;

    before: Date;

    executing: boolean;

    sortBy: Array<models.SortCriteria>;

}
