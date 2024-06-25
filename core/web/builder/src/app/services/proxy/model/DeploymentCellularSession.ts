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

export class DeploymentCellularSession {
    getFixupPrototypeFunction() { return DeploymentCellularSession.fixupPrototype; }

    static newInstance(model: Partial<DeploymentCellularSession>): DeploymentCellularSession {
        let obj = Object.assign(new DeploymentCellularSession(), model);
        DeploymentCellularSession.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DeploymentCellularSession>): DeploymentCellularSession {
        if (!model) return null;
        return DeploymentCellularSession.newInstance(<DeploymentCellularSession> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DeploymentCellularSession) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DeploymentCellularSession.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.packetsDownloaded === undefined) {
            this.packetsDownloaded = 0;
        }
        if (this.packetsUploaded === undefined) {
            this.packetsUploaded = 0;
        }
        if (this.estimatedLongitude === undefined) {
            this.estimatedLongitude = 0;
        }
        if (this.estimatedLatitude === undefined) {
            this.estimatedLatitude = 0;
        }
        if (typeof this.start === "string") {
            this.start = new Date(<string><any>this.start);
        }
        if (typeof this.end === "string") {
            this.end = new Date(<string><any>this.end);
        }
        if (typeof this.lastUpdated === "string") {
            this.lastUpdated = new Date(<string><any>this.lastUpdated);
        }
    }

    start: Date;

    end: Date;

    lastUpdated: Date;

    packetsDownloaded: number;

    packetsUploaded: number;

    cellId: string;

    operator: string;

    operatorCountry: string;

    radioLink: string;

    estimatedLongitude: number;

    estimatedLatitude: number;

}
