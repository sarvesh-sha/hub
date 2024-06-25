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

export class CustomerServiceAlertThresholds {
    getFixupPrototypeFunction() { return CustomerServiceAlertThresholds.fixupPrototype; }

    static newInstance(model: Partial<CustomerServiceAlertThresholds>): CustomerServiceAlertThresholds {
        let obj = Object.assign(new CustomerServiceAlertThresholds(), model);
        CustomerServiceAlertThresholds.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<CustomerServiceAlertThresholds>): CustomerServiceAlertThresholds {
        if (!model) return null;
        return CustomerServiceAlertThresholds.newInstance(<CustomerServiceAlertThresholds> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: CustomerServiceAlertThresholds) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.CustomerServiceAlertThresholds.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.warningThreshold === undefined) {
            this.warningThreshold = 0;
        }
        if (this.alertThreshold === undefined) {
            this.alertThreshold = 0;
        }
    }

    role: models.DeploymentRole;

    warningThreshold: number;

    alertThreshold: number;

}