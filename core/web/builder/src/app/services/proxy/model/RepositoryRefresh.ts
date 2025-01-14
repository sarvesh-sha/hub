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

export class RepositoryRefresh {
    getFixupPrototypeFunction() { return RepositoryRefresh.fixupPrototype; }

    static newInstance(model: Partial<RepositoryRefresh>): RepositoryRefresh {
        let obj = Object.assign(new RepositoryRefresh(), model);
        RepositoryRefresh.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<RepositoryRefresh>): RepositoryRefresh {
        if (!model) return null;
        return RepositoryRefresh.newInstance(<RepositoryRefresh> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: RepositoryRefresh) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.RepositoryRefresh.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.branchesAdded === undefined) {
            this.branchesAdded = 0;
        }
        if (this.branchesRemoved === undefined) {
            this.branchesRemoved = 0;
        }
        if (this.commitsAdded === undefined) {
            this.commitsAdded = 0;
        }
        if (this.commitsRemoved === undefined) {
            this.commitsRemoved = 0;
        }
    }

    status: models.BackgroundActivityStatus;

    branchesAdded: number;

    branchesRemoved: number;

    commitsAdded: number;

    commitsRemoved: number;

}
