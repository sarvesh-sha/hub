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

export class PointClassAssignment {
    getFixupPrototypeFunction() { return PointClassAssignment.fixupPrototype; }

    static newInstance(model: Partial<PointClassAssignment>): PointClassAssignment {
        let obj = Object.assign(new PointClassAssignment(), model);
        PointClassAssignment.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<PointClassAssignment>): PointClassAssignment {
        if (!model) return null;
        return PointClassAssignment.newInstance(<PointClassAssignment> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: PointClassAssignment) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.PointClassAssignment.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    regex: string;

    pointClassId: string;

    caseSensitive: boolean;

    comment: string;

}
