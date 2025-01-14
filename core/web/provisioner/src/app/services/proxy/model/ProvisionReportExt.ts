/**
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Provisioner APIs
 * APIs and Definitions for the Optio3 Provisioner product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

import * as models from './models';

export class ProvisionReportExt {
    getFixupPrototypeFunction() { return ProvisionReportExt.fixupPrototype; }

    static newInstance(model: Partial<ProvisionReportExt>): ProvisionReportExt {
        let obj = Object.assign(new ProvisionReportExt(), model);
        ProvisionReportExt.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ProvisionReportExt>): ProvisionReportExt {
        if (!model) return null;
        return ProvisionReportExt.newInstance(<ProvisionReportExt> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ProvisionReportExt) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.ProvisionReportExt.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.info) {
            models.ProvisionReport.fixupPrototype(this.info);
        }
    }

    info: models.ProvisionReport;

    printed: boolean;

    uploaded: boolean;

}
