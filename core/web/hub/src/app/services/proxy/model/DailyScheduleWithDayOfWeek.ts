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

export class DailyScheduleWithDayOfWeek {
    getFixupPrototypeFunction() { return DailyScheduleWithDayOfWeek.fixupPrototype; }

    static newInstance(model: Partial<DailyScheduleWithDayOfWeek>): DailyScheduleWithDayOfWeek {
        let obj = Object.assign(new DailyScheduleWithDayOfWeek(), model);
        DailyScheduleWithDayOfWeek.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DailyScheduleWithDayOfWeek>): DailyScheduleWithDayOfWeek {
        if (!model) return null;
        return DailyScheduleWithDayOfWeek.newInstance(<DailyScheduleWithDayOfWeek> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DailyScheduleWithDayOfWeek) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DailyScheduleWithDayOfWeek.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.dailySchedule) {
            models.DailySchedule.fixupPrototype(this.dailySchedule);
        }
    }

    dayOfWeek: models.DayOfWeek;

    dailySchedule: models.DailySchedule;

}