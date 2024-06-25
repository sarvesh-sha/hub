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

export class AssetGraphTransformRelationship extends models.AssetGraphTransform {
    constructor() {
        super();
        this.setDiscriminator("AssetGraphTransformRelationship");
    }

    getFixupPrototypeFunction() { return AssetGraphTransformRelationship.fixupPrototype; }

    static newInstance(model: Partial<AssetGraphTransformRelationship>): AssetGraphTransformRelationship {
        let obj = Object.assign(new AssetGraphTransformRelationship(), model);
        AssetGraphTransformRelationship.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AssetGraphTransformRelationship>): AssetGraphTransformRelationship {
        if (!model) return null;
        return AssetGraphTransformRelationship.newInstance(<AssetGraphTransformRelationship> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AssetGraphTransformRelationship) {
        models.AssetGraphTransform.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    relationship: models.AssetRelationship;

}
