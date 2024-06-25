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

export class TimeSeriesConfigurationBase {
    __type: string;

    protected setDiscriminator(type: string) {
        this.__type = type;
    }

    getFixupPrototypeFunction() { return TimeSeriesConfigurationBase.fixupPrototype; }

    static newInstance(model: Partial<TimeSeriesConfigurationBase>): TimeSeriesConfigurationBase {
        let obj = Object.assign(new TimeSeriesConfigurationBase(), model);
        TimeSeriesConfigurationBase.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TimeSeriesConfigurationBase>): TimeSeriesConfigurationBase {
        if (!model) return null;
        return TimeSeriesConfigurationBase.newInstance(<TimeSeriesConfigurationBase> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TimeSeriesConfigurationBase) {
        if (!obj) return;

        switch (obj.__type)
        {
            case "TimeSeriesChartConfiguration":
                Object.setPrototypeOf(obj, models.TimeSeriesChartConfiguration.prototype);
                break;
        }

        obj.fixupFields();
    }

    fixupFields() {
        if (this.version === undefined) {
            this.version = 0;
        }
    }

    version: number;

}