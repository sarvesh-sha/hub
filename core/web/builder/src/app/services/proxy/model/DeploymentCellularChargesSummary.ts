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

export class DeploymentCellularChargesSummary {
    getFixupPrototypeFunction() { return DeploymentCellularChargesSummary.fixupPrototype; }

    static newInstance(model: Partial<DeploymentCellularChargesSummary>): DeploymentCellularChargesSummary {
        let obj = Object.assign(new DeploymentCellularChargesSummary(), model);
        DeploymentCellularChargesSummary.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DeploymentCellularChargesSummary>): DeploymentCellularChargesSummary {
        if (!model) return null;
        return DeploymentCellularChargesSummary.newInstance(<DeploymentCellularChargesSummary> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DeploymentCellularChargesSummary) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DeploymentCellularChargesSummary.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.count === undefined) {
            this.count = 0;
        }
        if (this.last24Hours) {
            models.DeploymentCellularCharge.fixupPrototype(this.last24Hours);
        }
        if (this.last7Days) {
            models.DeploymentCellularCharge.fixupPrototype(this.last7Days);
        }
        if (this.last14Days) {
            models.DeploymentCellularCharge.fixupPrototype(this.last14Days);
        }
        if (this.last21Days) {
            models.DeploymentCellularCharge.fixupPrototype(this.last21Days);
        }
        if (this.last30Days) {
            models.DeploymentCellularCharge.fixupPrototype(this.last30Days);
        }
        if (this.last24HoursPerHost) {
            for (let val of this.last24HoursPerHost) {
                models.DeploymentCellularChargePerHost.fixupPrototype(val);
            }
        }
        if (this.last7DaysPerHost) {
            for (let val of this.last7DaysPerHost) {
                models.DeploymentCellularChargePerHost.fixupPrototype(val);
            }
        }
        if (this.last14DaysPerHost) {
            for (let val of this.last14DaysPerHost) {
                models.DeploymentCellularChargePerHost.fixupPrototype(val);
            }
        }
        if (this.last21DaysPerHost) {
            for (let val of this.last21DaysPerHost) {
                models.DeploymentCellularChargePerHost.fixupPrototype(val);
            }
        }
        if (this.last30DaysPerHost) {
            for (let val of this.last30DaysPerHost) {
                models.DeploymentCellularChargePerHost.fixupPrototype(val);
            }
        }
    }

    count: number;

    last24Hours: models.DeploymentCellularCharge;

    last7Days: models.DeploymentCellularCharge;

    last14Days: models.DeploymentCellularCharge;

    last21Days: models.DeploymentCellularCharge;

    last30Days: models.DeploymentCellularCharge;

    last24HoursPerHost: Array<models.DeploymentCellularChargePerHost>;

    last7DaysPerHost: Array<models.DeploymentCellularChargePerHost>;

    last14DaysPerHost: Array<models.DeploymentCellularChargePerHost>;

    last21DaysPerHost: Array<models.DeploymentCellularChargePerHost>;

    last30DaysPerHost: Array<models.DeploymentCellularChargePerHost>;

}
