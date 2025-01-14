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

export class DashboardGraphContextPreference {
    getFixupPrototypeFunction() { return DashboardGraphContextPreference.fixupPrototype; }

    static newInstance(model: Partial<DashboardGraphContextPreference>): DashboardGraphContextPreference {
        let obj = Object.assign(new DashboardGraphContextPreference(), model);
        DashboardGraphContextPreference.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DashboardGraphContextPreference>): DashboardGraphContextPreference {
        if (!model) return null;
        return DashboardGraphContextPreference.newInstance(<DashboardGraphContextPreference> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DashboardGraphContextPreference) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DashboardGraphContextPreference.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    graphContexts: { [key: string]: { [key: string]: string; }; };

}
