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

export class NormalizationEngineOperatorUnaryGetEquipmentClassName extends models.EngineOperatorUnaryFromNormalization {
    constructor() {
        super();
        this.setDiscriminator("NormalizationEngineOperatorUnaryGetEquipmentClassName");
    }

    getFixupPrototypeFunction() { return NormalizationEngineOperatorUnaryGetEquipmentClassName.fixupPrototype; }

    static newInstance(model: Partial<NormalizationEngineOperatorUnaryGetEquipmentClassName>): NormalizationEngineOperatorUnaryGetEquipmentClassName {
        let obj = Object.assign(new NormalizationEngineOperatorUnaryGetEquipmentClassName(), model);
        NormalizationEngineOperatorUnaryGetEquipmentClassName.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationEngineOperatorUnaryGetEquipmentClassName>): NormalizationEngineOperatorUnaryGetEquipmentClassName {
        if (!model) return null;
        return NormalizationEngineOperatorUnaryGetEquipmentClassName.newInstance(<NormalizationEngineOperatorUnaryGetEquipmentClassName> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationEngineOperatorUnaryGetEquipmentClassName) {
        models.EngineOperatorUnaryFromNormalization.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}