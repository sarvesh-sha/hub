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

export class AssetRelationshipRequest {
    getFixupPrototypeFunction() { return AssetRelationshipRequest.fixupPrototype; }

    static newInstance(model: Partial<AssetRelationshipRequest>): AssetRelationshipRequest {
        let obj = Object.assign(new AssetRelationshipRequest(), model);
        AssetRelationshipRequest.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AssetRelationshipRequest>): AssetRelationshipRequest {
        if (!model) return null;
        return AssetRelationshipRequest.newInstance(<AssetRelationshipRequest> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AssetRelationshipRequest) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.AssetRelationshipRequest.prototype);

        obj.fixupFields();
    }

    fixupFields() {
    }

    assetId: string;

    relationship: models.AssetRelationship;

    fromParentToChildren: boolean;

}
