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

export class TagsConditionEquipmentClass extends models.TagsCondition {
    constructor() {
        super();
        this.setDiscriminator("TagsConditionEquipmentClass");
    }

    getFixupPrototypeFunction() { return TagsConditionEquipmentClass.fixupPrototype; }

    static newInstance(model: Partial<TagsConditionEquipmentClass>): TagsConditionEquipmentClass {
        let obj = Object.assign(new TagsConditionEquipmentClass(), model);
        TagsConditionEquipmentClass.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TagsConditionEquipmentClass>): TagsConditionEquipmentClass {
        if (!model) return null;
        return TagsConditionEquipmentClass.newInstance(<TagsConditionEquipmentClass> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TagsConditionEquipmentClass) {
        models.TagsCondition.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    equipmentClass: string;

}
