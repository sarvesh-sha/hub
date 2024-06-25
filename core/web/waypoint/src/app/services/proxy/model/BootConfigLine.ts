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

export class BootConfigLine {
    getFixupPrototypeFunction() { return BootConfigLine.fixupPrototype; }

    static newInstance(model: Partial<BootConfigLine>): BootConfigLine {
        let obj = Object.assign(new BootConfigLine(), model);
        BootConfigLine.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<BootConfigLine>): BootConfigLine {
        if (!model) return null;
        return BootConfigLine.newInstance(<BootConfigLine> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: BootConfigLine) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.BootConfigLine.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    key: models.BootConfigOptions;

    keyRaw: string;

    value: string;

    isCommented: boolean;

}
