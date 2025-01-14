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

export class TagsConditionMetrics extends models.TagsCondition {
    constructor() {
        super();
        this.setDiscriminator("TagsConditionMetrics");
    }

    getFixupPrototypeFunction() { return TagsConditionMetrics.fixupPrototype; }

    static newInstance(model: Partial<TagsConditionMetrics>): TagsConditionMetrics {
        let obj = Object.assign(new TagsConditionMetrics(), model);
        TagsConditionMetrics.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TagsConditionMetrics>): TagsConditionMetrics {
        if (!model) return null;
        return TagsConditionMetrics.newInstance(<TagsConditionMetrics> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TagsConditionMetrics) {
        models.TagsCondition.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    metricsSysId: string;

}
