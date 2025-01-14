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

export class NormalizationEngineExpressionGetEquipment extends models.EngineExpressionFromNormalization {
    constructor() {
        super();
        this.setDiscriminator("NormalizationEngineExpressionGetEquipment");
    }

    getFixupPrototypeFunction() { return NormalizationEngineExpressionGetEquipment.fixupPrototype; }

    static newInstance(model: Partial<NormalizationEngineExpressionGetEquipment>): NormalizationEngineExpressionGetEquipment {
        let obj = Object.assign(new NormalizationEngineExpressionGetEquipment(), model);
        NormalizationEngineExpressionGetEquipment.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationEngineExpressionGetEquipment>): NormalizationEngineExpressionGetEquipment {
        if (!model) return null;
        return NormalizationEngineExpressionGetEquipment.newInstance(<NormalizationEngineExpressionGetEquipment> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationEngineExpressionGetEquipment) {
        models.EngineExpressionFromNormalization.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
