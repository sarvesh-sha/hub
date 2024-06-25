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

export class ServiceUpgradeLevel {
    getFixupPrototypeFunction() { return ServiceUpgradeLevel.fixupPrototype; }

    static newInstance(model: Partial<ServiceUpgradeLevel>): ServiceUpgradeLevel {
        let obj = Object.assign(new ServiceUpgradeLevel(), model);
        ServiceUpgradeLevel.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ServiceUpgradeLevel>): ServiceUpgradeLevel {
        if (!model) return null;
        return ServiceUpgradeLevel.newInstance(<ServiceUpgradeLevel> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ServiceUpgradeLevel) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.ServiceUpgradeLevel.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.service) {
            models.RecordIdentity.fixupPrototype(this.service);
        }
    }

    service: models.RecordIdentity;

    fixupProcessors: Array<string>;

}
