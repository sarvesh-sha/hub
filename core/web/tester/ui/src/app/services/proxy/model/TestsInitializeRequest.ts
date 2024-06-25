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

export class TestsInitializeRequest {
    getFixupPrototypeFunction() { return TestsInitializeRequest.fixupPrototype; }

    static newInstance(model: Partial<TestsInitializeRequest>): TestsInitializeRequest {
        let obj = Object.assign(new TestsInitializeRequest(), model);
        TestsInitializeRequest.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TestsInitializeRequest>): TestsInitializeRequest {
        if (!model) return null;
        return TestsInitializeRequest.newInstance(<TestsInitializeRequest> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TestsInitializeRequest) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.TestsInitializeRequest.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    url: string;

}