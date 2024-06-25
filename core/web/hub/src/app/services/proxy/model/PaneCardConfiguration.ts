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

export class PaneCardConfiguration {
    getFixupPrototypeFunction() { return PaneCardConfiguration.fixupPrototype; }

    static newInstance(model: Partial<PaneCardConfiguration>): PaneCardConfiguration {
        let obj = Object.assign(new PaneCardConfiguration(), model);
        PaneCardConfiguration.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<PaneCardConfiguration>): PaneCardConfiguration {
        if (!model) return null;
        return PaneCardConfiguration.newInstance(<PaneCardConfiguration> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: PaneCardConfiguration) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.PaneCardConfiguration.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.fields) {
            for (let val of this.fields) {
                models.PaneFieldConfiguration.fixupPrototype(val);
            }
        }
    }

    title: string;

    fields: Array<models.PaneFieldConfiguration>;

}