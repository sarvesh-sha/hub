/**
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Waypoint APIs
 * APIs and Definitions for the Optio3 Waypoint product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

import * as models from './models';

export class ProvisionTest {
    getFixupPrototypeFunction() { return ProvisionTest.fixupPrototype; }

    static newInstance(model: Partial<ProvisionTest>): ProvisionTest {
        let obj = Object.assign(new ProvisionTest(), model);
        ProvisionTest.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ProvisionTest>): ProvisionTest {
        if (!model) return null;
        return ProvisionTest.newInstance(<ProvisionTest> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ProvisionTest) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.ProvisionTest.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    name: string;

    result: models.ProvisionTestResult;

}
