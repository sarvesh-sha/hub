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

export class AlertTrendWidgetConfiguration extends models.AlertWidgetConfiguration {
    constructor() {
        super();
        this.setDiscriminator("AlertTrendWidgetConfiguration");
    }

    getFixupPrototypeFunction() { return AlertTrendWidgetConfiguration.fixupPrototype; }

    static newInstance(model: Partial<AlertTrendWidgetConfiguration>): AlertTrendWidgetConfiguration {
        let obj = Object.assign(new AlertTrendWidgetConfiguration(), model);
        AlertTrendWidgetConfiguration.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertTrendWidgetConfiguration>): AlertTrendWidgetConfiguration {
        if (!model) return null;
        return AlertTrendWidgetConfiguration.newInstance(<AlertTrendWidgetConfiguration> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertTrendWidgetConfiguration) {
        models.AlertWidgetConfiguration.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    frequency: models.AlertTrendFrequency;

}
