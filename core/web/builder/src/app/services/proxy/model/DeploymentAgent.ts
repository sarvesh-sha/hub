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

export class DeploymentAgent {
    static readonly RECORD_IDENTITY: string = "DeploymentAgent";

    getFixupPrototypeFunction() { return DeploymentAgent.fixupPrototype; }

    static newInstance(model: Partial<DeploymentAgent>): DeploymentAgent {
        let obj = Object.assign(new DeploymentAgent(), model);
        DeploymentAgent.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DeploymentAgent>): DeploymentAgent {
        if (!model) return null;
        return DeploymentAgent.newInstance(<DeploymentAgent> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DeploymentAgent) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DeploymentAgent.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (typeof this.createdOn === "string") {
            this.createdOn = new Date(<string><any>this.createdOn);
        }
        if (typeof this.updatedOn === "string") {
            this.updatedOn = new Date(<string><any>this.updatedOn);
        }
        if (typeof this.lastHeartbeat === "string") {
            this.lastHeartbeat = new Date(<string><any>this.lastHeartbeat);
        }
        if (this.deployment) {
            models.RecordIdentity.fixupPrototype(this.deployment);
        }
        if (this.details) {
            models.DeploymentAgentDetails.fixupPrototype(this.details);
        }
    }

    sysId: string;

    createdOn: Date;

    updatedOn: Date;

    lastHeartbeat: Date;

    deployment: models.RecordIdentity;

    instanceId: string;

    status: models.DeploymentStatus;

    active: boolean;

    dockerId: string;

    details: models.DeploymentAgentDetails;

}
