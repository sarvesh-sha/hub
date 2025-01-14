/**
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Hub APIs
 * APIs and Definitions for the Optio3 Hub product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

import * as models from './models';

export class DeviceHealthAggregate {
    getFixupPrototypeFunction() { return DeviceHealthAggregate.fixupPrototype; }

    static newInstance(model: Partial<DeviceHealthAggregate>): DeviceHealthAggregate {
        let obj = Object.assign(new DeviceHealthAggregate(), model);
        DeviceHealthAggregate.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DeviceHealthAggregate>): DeviceHealthAggregate {
        if (!model) return null;
        return DeviceHealthAggregate.newInstance(<DeviceHealthAggregate> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DeviceHealthAggregate) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DeviceHealthAggregate.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.count === undefined) {
            this.count = 0;
        }
    }

    type: models.AlertType;

    maxSeverity: models.AlertSeverity;

    count: number;

}
