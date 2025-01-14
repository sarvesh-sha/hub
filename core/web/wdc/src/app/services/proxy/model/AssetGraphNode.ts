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

export class AssetGraphNode {
    getFixupPrototypeFunction() { return AssetGraphNode.fixupPrototype; }

    static newInstance(model: Partial<AssetGraphNode>): AssetGraphNode {
        let obj = Object.assign(new AssetGraphNode(), model);
        AssetGraphNode.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AssetGraphNode>): AssetGraphNode {
        if (!model) return null;
        return AssetGraphNode.newInstance(<AssetGraphNode> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AssetGraphNode) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.AssetGraphNode.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.condition) {
            models.TagsCondition.fixupPrototype(this.condition);
        }
    }

    id: string;

    name: string;

    optional: boolean;

    allowMultiple: boolean;

    condition: models.TagsCondition;

}
