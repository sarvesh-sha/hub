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

export class ReportSchedule {
    __type: string;

    protected setDiscriminator(type: string) {
        this.__type = type;
    }

    getFixupPrototypeFunction() { return ReportSchedule.fixupPrototype; }

    static newInstance(model: Partial<ReportSchedule>): ReportSchedule {
        let obj = Object.assign(new ReportSchedule(), model);
        ReportSchedule.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ReportSchedule>): ReportSchedule {
        if (!model) return null;
        return ReportSchedule.newInstance(<ReportSchedule> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ReportSchedule) {
        if (!obj) return;

        switch (obj.__type)
        {
            case "ReportScheduleDaily":
                Object.setPrototypeOf(obj, models.ReportScheduleDaily.prototype);
                break;
            case "ReportScheduleMonthly":
                Object.setPrototypeOf(obj, models.ReportScheduleMonthly.prototype);
                break;
            case "ReportScheduleOnDemand":
                Object.setPrototypeOf(obj, models.ReportScheduleOnDemand.prototype);
                break;
            case "ReportScheduleWeekly":
                Object.setPrototypeOf(obj, models.ReportScheduleWeekly.prototype);
                break;
        }

        obj.fixupFields();
    }

    fixupFields() {
        if (this.hour === undefined) {
            this.hour = 0;
        }
        if (this.minute === undefined) {
            this.minute = 0;
        }
    }

    hour: number;

    minute: number;

    zoneDesired: string;

}
