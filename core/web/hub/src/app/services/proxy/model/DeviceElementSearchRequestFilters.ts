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

export class DeviceElementSearchRequestFilters extends models.SearchRequestFilters {
    constructor() {
        super();
        this.setDiscriminator("DeviceElementSearchRequestFilters");
    }

    getFixupPrototypeFunction() { return DeviceElementSearchRequestFilters.fixupPrototype; }

    static newInstance(model: Partial<DeviceElementSearchRequestFilters>): DeviceElementSearchRequestFilters {
        let obj = Object.assign(new DeviceElementSearchRequestFilters(), model);
        DeviceElementSearchRequestFilters.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DeviceElementSearchRequestFilters>): DeviceElementSearchRequestFilters {
        if (!model) return null;
        return DeviceElementSearchRequestFilters.newInstance(<DeviceElementSearchRequestFilters> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DeviceElementSearchRequestFilters) {
        models.SearchRequestFilters.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    hasAnySampling: boolean;

    hasNoSampling: boolean;

    isClassified: boolean;

    isUnclassified: boolean;

    isHidden: boolean;

    isNotHidden: boolean;

}
