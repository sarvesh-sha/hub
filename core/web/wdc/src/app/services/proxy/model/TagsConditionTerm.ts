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

export class TagsConditionTerm extends models.TagsCondition {
    constructor() {
        super();
        this.setDiscriminator("TagsConditionTerm");
    }

    getFixupPrototypeFunction() { return TagsConditionTerm.fixupPrototype; }

    static newInstance(model: Partial<TagsConditionTerm>): TagsConditionTerm {
        let obj = Object.assign(new TagsConditionTerm(), model);
        TagsConditionTerm.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TagsConditionTerm>): TagsConditionTerm {
        if (!model) return null;
        return TagsConditionTerm.newInstance(<TagsConditionTerm> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TagsConditionTerm) {
        models.TagsCondition.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    tag: string;

}
