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

export class DeploymentCellularCharge {
    getFixupPrototypeFunction() { return DeploymentCellularCharge.fixupPrototype; }

    static newInstance(model: Partial<DeploymentCellularCharge>): DeploymentCellularCharge {
        let obj = Object.assign(new DeploymentCellularCharge(), model);
        DeploymentCellularCharge.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DeploymentCellularCharge>): DeploymentCellularCharge {
        if (!model) return null;
        return DeploymentCellularCharge.newInstance(<DeploymentCellularCharge> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DeploymentCellularCharge) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DeploymentCellularCharge.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.upload === undefined) {
            this.upload = 0;
        }
        if (this.download === undefined) {
            this.download = 0;
        }
        if (this.total === undefined) {
            this.total = 0;
        }
        if (this.billed === undefined) {
            this.billed = 0;
        }
        if (this.fees === undefined) {
            this.fees = 0;
        }
        if (this.feesOverage === undefined) {
            this.feesOverage = 0;
        }
        if (typeof this.timestamp === "string") {
            this.timestamp = new Date(<string><any>this.timestamp);
        }
    }

    timestamp: Date;

    upload: number;

    download: number;

    total: number;

    billed: number;

    fees: number;

    feesOverage: number;

}