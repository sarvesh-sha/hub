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

export class MetricsDefinitionFilterRequest {
    getFixupPrototypeFunction() { return MetricsDefinitionFilterRequest.fixupPrototype; }

    static newInstance(model: Partial<MetricsDefinitionFilterRequest>): MetricsDefinitionFilterRequest {
        let obj = Object.assign(new MetricsDefinitionFilterRequest(), model);
        MetricsDefinitionFilterRequest.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MetricsDefinitionFilterRequest>): MetricsDefinitionFilterRequest {
        if (!model) return null;
        return MetricsDefinitionFilterRequest.newInstance(<MetricsDefinitionFilterRequest> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MetricsDefinitionFilterRequest) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.MetricsDefinitionFilterRequest.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.sortBy) {
            for (let val of this.sortBy) {
                models.SortCriteria.fixupPrototype(val);
            }
        }
    }

    sortBy: Array<models.SortCriteria>;

}
