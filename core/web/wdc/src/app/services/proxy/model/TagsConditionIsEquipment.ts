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

export class TagsConditionIsEquipment extends models.TagsCondition {
    constructor() {
        super();
        this.setDiscriminator("TagsConditionIsEquipment");
    }

    getFixupPrototypeFunction() { return TagsConditionIsEquipment.fixupPrototype; }

    static newInstance(model: Partial<TagsConditionIsEquipment>): TagsConditionIsEquipment {
        let obj = Object.assign(new TagsConditionIsEquipment(), model);
        TagsConditionIsEquipment.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TagsConditionIsEquipment>): TagsConditionIsEquipment {
        if (!model) return null;
        return TagsConditionIsEquipment.newInstance(<TagsConditionIsEquipment> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TagsConditionIsEquipment) {
        models.TagsCondition.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
