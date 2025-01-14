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

export class DeploymentTaskConfiguration {
    getFixupPrototypeFunction() { return DeploymentTaskConfiguration.fixupPrototype; }

    static newInstance(model: Partial<DeploymentTaskConfiguration>): DeploymentTaskConfiguration {
        let obj = Object.assign(new DeploymentTaskConfiguration(), model);
        DeploymentTaskConfiguration.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DeploymentTaskConfiguration>): DeploymentTaskConfiguration {
        if (!model) return null;
        return DeploymentTaskConfiguration.newInstance(<DeploymentTaskConfiguration> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DeploymentTaskConfiguration) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DeploymentTaskConfiguration.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    privileged: boolean;

    useHostNetwork: boolean;

    entrypoint: string;

    commandLine: string;

}
