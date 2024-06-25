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

export class SearchRequest {
    getFixupPrototypeFunction() { return SearchRequest.fixupPrototype; }

    static newInstance(model: Partial<SearchRequest>): SearchRequest {
        let obj = Object.assign(new SearchRequest(), model);
        SearchRequest.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<SearchRequest>): SearchRequest {
        if (!model) return null;
        return SearchRequest.newInstance(<SearchRequest> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: SearchRequest) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.SearchRequest.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.filters) {
            for (let val of this.filters) {
                models.SearchRequestFilters.fixupPrototype(val);
            }
        }
    }

    query: string;

    filters: Array<models.SearchRequestFilters>;

    scopeToFilters: boolean;

}
