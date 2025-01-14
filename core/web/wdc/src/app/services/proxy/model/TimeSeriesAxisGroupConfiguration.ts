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

export class TimeSeriesAxisGroupConfiguration {
    getFixupPrototypeFunction() { return TimeSeriesAxisGroupConfiguration.fixupPrototype; }

    static newInstance(model: Partial<TimeSeriesAxisGroupConfiguration>): TimeSeriesAxisGroupConfiguration {
        let obj = Object.assign(new TimeSeriesAxisGroupConfiguration(), model);
        TimeSeriesAxisGroupConfiguration.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TimeSeriesAxisGroupConfiguration>): TimeSeriesAxisGroupConfiguration {
        if (!model) return null;
        return TimeSeriesAxisGroupConfiguration.newInstance(<TimeSeriesAxisGroupConfiguration> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TimeSeriesAxisGroupConfiguration) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.TimeSeriesAxisGroupConfiguration.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.keyFactors) {
            models.EngineeringUnitsFactors.fixupPrototype(this.keyFactors);
        }
        if (this.selectedFactors) {
            models.EngineeringUnitsFactors.fixupPrototype(this.selectedFactors);
        }
        if (this.override) {
            models.ToggleableNumericRange.fixupPrototype(this.override);
        }
    }

    keyFactors: models.EngineeringUnitsFactors;

    selectedFactors: models.EngineeringUnitsFactors;

    override: models.ToggleableNumericRange;

}
