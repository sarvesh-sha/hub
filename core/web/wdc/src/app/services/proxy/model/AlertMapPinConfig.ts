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

export class AlertMapPinConfig {
    getFixupPrototypeFunction() { return AlertMapPinConfig.fixupPrototype; }

    static newInstance(model: Partial<AlertMapPinConfig>): AlertMapPinConfig {
        let obj = Object.assign(new AlertMapPinConfig(), model);
        AlertMapPinConfig.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertMapPinConfig>): AlertMapPinConfig {
        if (!model) return null;
        return AlertMapPinConfig.newInstance(<AlertMapPinConfig> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertMapPinConfig) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.AlertMapPinConfig.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.pinSize === undefined) {
            this.pinSize = 0;
        }
        if (this.countColors) {
            for (let val of this.countColors) {
                models.ColorSegment.fixupPrototype(val);
            }
        }
        if (this.severityColors) {
            for (let val of this.severityColors) {
                models.AlertMapSeverityColor.fixupPrototype(val);
            }
        }
    }

    pinIcon: models.MapPinIcon;

    pinSize: number;

    colorMode: models.AlertMapPinColorMode;

    dataSource: models.AlertMapPinDataSource;

    staticColor: string;

    countColors: Array<models.ColorSegment>;

    severityColors: Array<models.AlertMapSeverityColor>;

}
