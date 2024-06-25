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

export class TagsConditionIsClassified extends models.TagsCondition {
    constructor() {
        super();
        this.setDiscriminator("TagsConditionIsClassified");
    }

    getFixupPrototypeFunction() { return TagsConditionIsClassified.fixupPrototype; }

    static newInstance(model: Partial<TagsConditionIsClassified>): TagsConditionIsClassified {
        let obj = Object.assign(new TagsConditionIsClassified(), model);
        TagsConditionIsClassified.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TagsConditionIsClassified>): TagsConditionIsClassified {
        if (!model) return null;
        return TagsConditionIsClassified.newInstance(<TagsConditionIsClassified> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TagsConditionIsClassified) {
        models.TagsCondition.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
