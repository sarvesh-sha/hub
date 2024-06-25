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

export class PaneFieldConfigurationCurrentValue extends models.PaneFieldConfiguration {
    constructor() {
        super();
        this.setDiscriminator("PaneFieldConfigurationCurrentValue");
    }

    getFixupPrototypeFunction() { return PaneFieldConfigurationCurrentValue.fixupPrototype; }

    static newInstance(model: Partial<PaneFieldConfigurationCurrentValue>): PaneFieldConfigurationCurrentValue {
        let obj = Object.assign(new PaneFieldConfigurationCurrentValue(), model);
        PaneFieldConfigurationCurrentValue.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<PaneFieldConfigurationCurrentValue>): PaneFieldConfigurationCurrentValue {
        if (!model) return null;
        return PaneFieldConfigurationCurrentValue.newInstance(<PaneFieldConfigurationCurrentValue> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: PaneFieldConfigurationCurrentValue) {
        models.PaneFieldConfiguration.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.unitsFactors) {
            models.EngineeringUnitsFactors.fixupPrototype(this.unitsFactors);
        }
        if (this.pointInput) {
            models.AssetGraphBinding.fixupPrototype(this.pointInput);
        }
    }

    unitsFactors: models.EngineeringUnitsFactors;

    suffix: string;

    pointInput: models.AssetGraphBinding;

}
