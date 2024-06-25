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

export class ReportConfiguration {
    getFixupPrototypeFunction() { return ReportConfiguration.fixupPrototype; }

    static newInstance(model: Partial<ReportConfiguration>): ReportConfiguration {
        let obj = Object.assign(new ReportConfiguration(), model);
        ReportConfiguration.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ReportConfiguration>): ReportConfiguration {
        if (!model) return null;
        return ReportConfiguration.newInstance(<ReportConfiguration> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ReportConfiguration) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.ReportConfiguration.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.container) {
            models.ReportLayoutBase.fixupPrototype(this.container);
        }
        if (this.sharedGraphs) {
            for (let val of this.sharedGraphs) {
                models.SharedAssetGraph.fixupPrototype(val);
            }
        }
    }

    sharedGraphs: Array<models.SharedAssetGraph>;

    pdfFormat: models.PaperFormat;

    landscape: boolean;

    container: models.ReportLayoutBase;

}
