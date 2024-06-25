/**
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Api documentation
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

import * as models from './models';

export class TestResult {
    getFixupPrototypeFunction() { return TestResult.fixupPrototype; }

    static newInstance(model: Partial<TestResult>): TestResult {
        let obj = Object.assign(new TestResult(), model);
        TestResult.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TestResult>): TestResult {
        if (!model) return null;
        return TestResult.newInstance(<TestResult> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TestResult) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.TestResult.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    id: string;

    name: string;

    message: string;

    status: models.TestResultStatus;

    logs: Array<string>;

    consoleLogs: Array<string>;

    testStart: string;

    testEnd: string;

    videoId: string;

}