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

export class CustomerServiceDesiredStateRole {
    getFixupPrototypeFunction() { return CustomerServiceDesiredStateRole.fixupPrototype; }

    static newInstance(model: Partial<CustomerServiceDesiredStateRole>): CustomerServiceDesiredStateRole {
        let obj = Object.assign(new CustomerServiceDesiredStateRole(), model);
        CustomerServiceDesiredStateRole.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<CustomerServiceDesiredStateRole>): CustomerServiceDesiredStateRole {
        if (!model) return null;
        return CustomerServiceDesiredStateRole.newInstance(<CustomerServiceDesiredStateRole> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: CustomerServiceDesiredStateRole) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.CustomerServiceDesiredStateRole.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.image) {
            models.RecordIdentity.fixupPrototype(this.image);
        }
    }

    role: models.DeploymentRole;

    architecture: models.DockerImageArchitecture;

    image: models.RecordIdentity;

    shutdown: boolean;

    shutdownIfDifferent: boolean;

    launch: boolean;

    launchIfMissing: boolean;

    launchIfMissingAndIdle: boolean;

}
