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

export class TagsJoinQuery {
    getFixupPrototypeFunction() { return TagsJoinQuery.fixupPrototype; }

    static newInstance(model: Partial<TagsJoinQuery>): TagsJoinQuery {
        let obj = Object.assign(new TagsJoinQuery(), model);
        TagsJoinQuery.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TagsJoinQuery>): TagsJoinQuery {
        if (!model) return null;
        return TagsJoinQuery.newInstance(<TagsJoinQuery> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TagsJoinQuery) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.TagsJoinQuery.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.startOffset === undefined) {
            this.startOffset = 0;
        }
        if (this.maxResults === undefined) {
            this.maxResults = 0;
        }
        if (this.terms) {
            for (let val of this.terms) {
                models.TagsJoinTerm.fixupPrototype(val);
            }
        }
        if (this.joins) {
            for (let val of this.joins) {
                models.TagsJoin.fixupPrototype(val);
            }
        }
    }

    terms: Array<models.TagsJoinTerm>;

    joins: Array<models.TagsJoin>;

    startOffset: number;

    maxResults: number;

}