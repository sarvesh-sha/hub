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

export class TagsSummary {
    getFixupPrototypeFunction() { return TagsSummary.fixupPrototype; }

    static newInstance(model: Partial<TagsSummary>): TagsSummary {
        let obj = Object.assign(new TagsSummary(), model);
        TagsSummary.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<TagsSummary>): TagsSummary {
        if (!model) return null;
        return TagsSummary.newInstance(<TagsSummary> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: TagsSummary) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.TagsSummary.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.version === undefined) {
            this.version = 0;
        }
    }

    version: number;

    tagFrequency: { [key: string]: number; };

    relationFrequency: { [key: string]: number; };

    pointClassesFrequency: { [key: string]: number; };

    equipmentClassesFrequency: { [key: string]: number; };

}