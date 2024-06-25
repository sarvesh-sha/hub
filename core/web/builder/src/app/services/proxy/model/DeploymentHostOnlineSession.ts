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

export class DeploymentHostOnlineSession {
    getFixupPrototypeFunction() { return DeploymentHostOnlineSession.fixupPrototype; }

    static newInstance(model: Partial<DeploymentHostOnlineSession>): DeploymentHostOnlineSession {
        let obj = Object.assign(new DeploymentHostOnlineSession(), model);
        DeploymentHostOnlineSession.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DeploymentHostOnlineSession>): DeploymentHostOnlineSession {
        if (!model) return null;
        return DeploymentHostOnlineSession.newInstance(<DeploymentHostOnlineSession> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DeploymentHostOnlineSession) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DeploymentHostOnlineSession.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (typeof this.start === "string") {
            this.start = new Date(<string><any>this.start);
        }
        if (typeof this.end === "string") {
            this.end = new Date(<string><any>this.end);
        }
    }

    start: Date;

    end: Date;

}