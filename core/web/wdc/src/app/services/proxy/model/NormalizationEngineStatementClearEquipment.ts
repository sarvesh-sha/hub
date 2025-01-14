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

export class NormalizationEngineStatementClearEquipment extends models.EngineStatementFromNormalization {
    constructor() {
        super();
        this.setDiscriminator("NormalizationEngineStatementClearEquipment");
    }

    getFixupPrototypeFunction() { return NormalizationEngineStatementClearEquipment.fixupPrototype; }

    static newInstance(model: Partial<NormalizationEngineStatementClearEquipment>): NormalizationEngineStatementClearEquipment {
        let obj = Object.assign(new NormalizationEngineStatementClearEquipment(), model);
        NormalizationEngineStatementClearEquipment.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationEngineStatementClearEquipment>): NormalizationEngineStatementClearEquipment {
        if (!model) return null;
        return NormalizationEngineStatementClearEquipment.newInstance(<NormalizationEngineStatementClearEquipment> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationEngineStatementClearEquipment) {
        models.EngineStatementFromNormalization.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
