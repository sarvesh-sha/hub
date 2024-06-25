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

export class RepositoryCommit {
    static readonly RECORD_IDENTITY: string = "RepositoryCommit";

    getFixupPrototypeFunction() { return RepositoryCommit.fixupPrototype; }

    static newInstance(model: Partial<RepositoryCommit>): RepositoryCommit {
        let obj = Object.assign(new RepositoryCommit(), model);
        RepositoryCommit.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<RepositoryCommit>): RepositoryCommit {
        if (!model) return null;
        return RepositoryCommit.newInstance(<RepositoryCommit> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: RepositoryCommit) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.RepositoryCommit.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (typeof this.createdOn === "string") {
            this.createdOn = new Date(<string><any>this.createdOn);
        }
        if (typeof this.updatedOn === "string") {
            this.updatedOn = new Date(<string><any>this.updatedOn);
        }
        if (this.repository) {
            models.RecordIdentity.fixupPrototype(this.repository);
        }
    }

    sysId: string;

    createdOn: Date;

    updatedOn: Date;

    repository: models.RecordIdentity;

    commitHash: string;

    message: string;

    authorName: string;

    authorEmailAddress: string;

    parents: Array<string>;

}