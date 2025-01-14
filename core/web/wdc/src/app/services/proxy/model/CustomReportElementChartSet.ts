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

export class CustomReportElementChartSet extends models.CustomReportElement {
    constructor() {
        super();
        this.setDiscriminator("CustomReportElementChartSet");
    }

    getFixupPrototypeFunction() { return CustomReportElementChartSet.fixupPrototype; }

    static newInstance(model: Partial<CustomReportElementChartSet>): CustomReportElementChartSet {
        let obj = Object.assign(new CustomReportElementChartSet(), model);
        CustomReportElementChartSet.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<CustomReportElementChartSet>): CustomReportElementChartSet {
        if (!model) return null;
        return CustomReportElementChartSet.newInstance(<CustomReportElementChartSet> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: CustomReportElementChartSet) {
        models.CustomReportElement.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.charts) {
            for (let val of this.charts) {
                models.TimeSeriesConfigurationBase.fixupPrototype(val);
            }
        }
    }

    charts: Array<models.TimeSeriesChartConfiguration>;

}
