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

export class AssetGraphTransform {
    __type: string;

    protected setDiscriminator(type: string) {
        this.__type = type;
    }

    getFixupPrototypeFunction() { return AssetGraphTransform.fixupPrototype; }

    static newInstance(model: Partial<AssetGraphTransform>): AssetGraphTransform {
        let obj = Object.assign(new AssetGraphTransform(), model);
        AssetGraphTransform.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AssetGraphTransform>): AssetGraphTransform {
        if (!model) return null;
        return AssetGraphTransform.newInstance(<AssetGraphTransform> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AssetGraphTransform) {
        if (!obj) return;

        switch (obj.__type)
        {
            case "AssetGraphTransformRelationship":
                Object.setPrototypeOf(obj, models.AssetGraphTransformRelationship.prototype);
                break;
        }

        obj.fixupFields();
    }

    fixupFields() {
    }

    inputId: string;

    outputId: string;

}
