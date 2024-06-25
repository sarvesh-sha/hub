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

export class ShellOutput {
    getFixupPrototypeFunction() { return ShellOutput.fixupPrototype; }

    static newInstance(model: Partial<ShellOutput>): ShellOutput {
        let obj = Object.assign(new ShellOutput(), model);
        ShellOutput.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ShellOutput>): ShellOutput {
        if (!model) return null;
        return ShellOutput.newInstance(<ShellOutput> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ShellOutput) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.ShellOutput.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.fd === undefined) {
            this.fd = 0;
        }
        if (typeof this.timestamp === "string") {
            this.timestamp = new Date(<string><any>this.timestamp);
        }
    }

    timestamp: Date;

    fd: number;

    payload: string;

}