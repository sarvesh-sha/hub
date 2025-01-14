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

export class PaneFieldChart extends models.PaneField {
    constructor() {
        super();
        this.setDiscriminator("PaneFieldChart");
    }

    getFixupPrototypeFunction() { return PaneFieldChart.fixupPrototype; }

    static newInstance(model: Partial<PaneFieldChart>): PaneFieldChart {
        let obj = Object.assign(new PaneFieldChart(), model);
        PaneFieldChart.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<PaneFieldChart>): PaneFieldChart {
        if (!model) return null;
        return PaneFieldChart.newInstance(<PaneFieldChart> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: PaneFieldChart) {
        models.PaneField.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.value) {
            models.TimeSeriesConfigurationBase.fixupPrototype(this.value);
        }
    }

    value: models.TimeSeriesChartConfiguration;

}
