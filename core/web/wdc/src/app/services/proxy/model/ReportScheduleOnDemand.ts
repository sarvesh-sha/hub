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

export class ReportScheduleOnDemand extends models.ReportSchedule {
    constructor() {
        super();
        this.setDiscriminator("ReportScheduleOnDemand");
    }

    getFixupPrototypeFunction() { return ReportScheduleOnDemand.fixupPrototype; }

    static newInstance(model: Partial<ReportScheduleOnDemand>): ReportScheduleOnDemand {
        let obj = Object.assign(new ReportScheduleOnDemand(), model);
        ReportScheduleOnDemand.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ReportScheduleOnDemand>): ReportScheduleOnDemand {
        if (!model) return null;
        return ReportScheduleOnDemand.newInstance(<ReportScheduleOnDemand> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ReportScheduleOnDemand) {
        models.ReportSchedule.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}