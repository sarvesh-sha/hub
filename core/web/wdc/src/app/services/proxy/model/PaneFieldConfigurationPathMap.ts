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

export class PaneFieldConfigurationPathMap extends models.PaneFieldConfiguration {
    constructor() {
        super();
        this.setDiscriminator("PaneFieldConfigurationPathMap");
    }

    getFixupPrototypeFunction() { return PaneFieldConfigurationPathMap.fixupPrototype; }

    static newInstance(model: Partial<PaneFieldConfigurationPathMap>): PaneFieldConfigurationPathMap {
        let obj = Object.assign(new PaneFieldConfigurationPathMap(), model);
        PaneFieldConfigurationPathMap.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<PaneFieldConfigurationPathMap>): PaneFieldConfigurationPathMap {
        if (!model) return null;
        return PaneFieldConfigurationPathMap.newInstance(<PaneFieldConfigurationPathMap> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: PaneFieldConfigurationPathMap) {
        models.PaneFieldConfiguration.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.locationInput) {
            models.AssetGraphBinding.fixupPrototype(this.locationInput);
        }
    }

    locationInput: models.AssetGraphBinding;

}
