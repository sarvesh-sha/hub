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

export class NormalizationEngineStatementPushEquipmentWithClass extends models.EngineStatementFromNormalization {
    constructor() {
        super();
        this.setDiscriminator("NormalizationEngineStatementPushEquipmentWithClass");
    }

    getFixupPrototypeFunction() { return NormalizationEngineStatementPushEquipmentWithClass.fixupPrototype; }

    static newInstance(model: Partial<NormalizationEngineStatementPushEquipmentWithClass>): NormalizationEngineStatementPushEquipmentWithClass {
        let obj = Object.assign(new NormalizationEngineStatementPushEquipmentWithClass(), model);
        NormalizationEngineStatementPushEquipmentWithClass.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationEngineStatementPushEquipmentWithClass>): NormalizationEngineStatementPushEquipmentWithClass {
        if (!model) return null;
        return NormalizationEngineStatementPushEquipmentWithClass.newInstance(<NormalizationEngineStatementPushEquipmentWithClass> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationEngineStatementPushEquipmentWithClass) {
        models.EngineStatementFromNormalization.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.value) {
            models.EngineBlock.fixupPrototype(this.value);
        }
    }

    value: models.EngineExpression;

    equipmentClassId: string;

}
