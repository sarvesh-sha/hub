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

export class TestsRunRequest {
    getFixupPrototypeFunction() { return TestsRunRequest.fixupPrototype; }

    static newInstance(model: Partial<TestsRunRequest>): TestsRunRequest {
        let obj = Object.assign(new TestsRunRequest(), model);
        TestsRunRequest.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TestsRunRequest>): TestsRunRequest {
        if (!model) return null;
        return TestsRunRequest.newInstance(<TestsRunRequest> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TestsRunRequest) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.TestsRunRequest.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    url: string;

    ids: Array<string>;

    categories: Array<string>;

}