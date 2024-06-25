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

export class EngineeringUnitsPreference {
    getFixupPrototypeFunction() { return EngineeringUnitsPreference.fixupPrototype; }

    static newInstance(model: Partial<EngineeringUnitsPreference>): EngineeringUnitsPreference {
        let obj = Object.assign(new EngineeringUnitsPreference(), model);
        EngineeringUnitsPreference.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineeringUnitsPreference>): EngineeringUnitsPreference {
        if (!model) return null;
        return EngineeringUnitsPreference.newInstance(<EngineeringUnitsPreference> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineeringUnitsPreference) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.EngineeringUnitsPreference.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.units) {
            for (let val of this.units) {
                models.EngineeringUnitsPreferencePair.fixupPrototype(val);
            }
        }
    }

    units: Array<models.EngineeringUnitsPreferencePair>;

}
