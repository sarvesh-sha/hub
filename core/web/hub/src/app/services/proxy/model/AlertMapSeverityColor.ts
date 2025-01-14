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

export class AlertMapSeverityColor {
    getFixupPrototypeFunction() { return AlertMapSeverityColor.fixupPrototype; }

    static newInstance(model: Partial<AlertMapSeverityColor>): AlertMapSeverityColor {
        let obj = Object.assign(new AlertMapSeverityColor(), model);
        AlertMapSeverityColor.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertMapSeverityColor>): AlertMapSeverityColor {
        if (!model) return null;
        return AlertMapSeverityColor.newInstance(<AlertMapSeverityColor> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertMapSeverityColor) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.AlertMapSeverityColor.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    severity: models.AlertSeverity;

    color: string;

}
