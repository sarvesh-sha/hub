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

export class RecurringWeeklySchedule {
    getFixupPrototypeFunction() { return RecurringWeeklySchedule.fixupPrototype; }

    static newInstance(model: Partial<RecurringWeeklySchedule>): RecurringWeeklySchedule {
        let obj = Object.assign(new RecurringWeeklySchedule(), model);
        RecurringWeeklySchedule.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<RecurringWeeklySchedule>): RecurringWeeklySchedule {
        if (!model) return null;
        return RecurringWeeklySchedule.newInstance(<RecurringWeeklySchedule> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: RecurringWeeklySchedule) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.RecurringWeeklySchedule.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.days) {
            for (let val of this.days) {
                models.DailyScheduleWithDayOfWeek.fixupPrototype(val);
            }
        }
    }

    days: Array<models.DailyScheduleWithDayOfWeek>;

}
