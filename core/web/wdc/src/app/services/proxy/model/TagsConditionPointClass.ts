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

export class TagsConditionPointClass extends models.TagsCondition {
    constructor() {
        super();
        this.setDiscriminator("TagsConditionPointClass");
    }

    getFixupPrototypeFunction() { return TagsConditionPointClass.fixupPrototype; }

    static newInstance(model: Partial<TagsConditionPointClass>): TagsConditionPointClass {
        let obj = Object.assign(new TagsConditionPointClass(), model);
        TagsConditionPointClass.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TagsConditionPointClass>): TagsConditionPointClass {
        if (!model) return null;
        return TagsConditionPointClass.newInstance(<TagsConditionPointClass> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TagsConditionPointClass) {
        models.TagsCondition.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    pointClass: string;

}
