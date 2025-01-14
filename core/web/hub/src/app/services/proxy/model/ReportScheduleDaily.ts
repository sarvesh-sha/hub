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

export class ReportScheduleDaily extends models.ReportSchedule {
    constructor() {
        super();
        this.setDiscriminator("ReportScheduleDaily");
    }

    getFixupPrototypeFunction() { return ReportScheduleDaily.fixupPrototype; }

    static newInstance(model: Partial<ReportScheduleDaily>): ReportScheduleDaily {
        let obj = Object.assign(new ReportScheduleDaily(), model);
        ReportScheduleDaily.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ReportScheduleDaily>): ReportScheduleDaily {
        if (!model) return null;
        return ReportScheduleDaily.newInstance(<ReportScheduleDaily> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ReportScheduleDaily) {
        models.ReportSchedule.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    days: Array<models.DayOfWeek>;

}
