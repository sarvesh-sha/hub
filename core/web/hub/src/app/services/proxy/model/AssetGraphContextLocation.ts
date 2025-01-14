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

export class AssetGraphContextLocation extends models.AssetGraphContext {
    constructor() {
        super();
        this.setDiscriminator("AssetGraphContextLocation");
    }

    getFixupPrototypeFunction() { return AssetGraphContextLocation.fixupPrototype; }

    static newInstance(model: Partial<AssetGraphContextLocation>): AssetGraphContextLocation {
        let obj = Object.assign(new AssetGraphContextLocation(), model);
        AssetGraphContextLocation.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AssetGraphContextLocation>): AssetGraphContextLocation {
        if (!model) return null;
        return AssetGraphContextLocation.newInstance(<AssetGraphContextLocation> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AssetGraphContextLocation) {
        models.AssetGraphContext.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    locationSysId: string;

}
