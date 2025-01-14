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

export class NormalizationEngineStatementPushEquipmentLocation extends models.EngineStatementFromNormalization {
    constructor() {
        super();
        this.setDiscriminator("NormalizationEngineStatementPushEquipmentLocation");
    }

    getFixupPrototypeFunction() { return NormalizationEngineStatementPushEquipmentLocation.fixupPrototype; }

    static newInstance(model: Partial<NormalizationEngineStatementPushEquipmentLocation>): NormalizationEngineStatementPushEquipmentLocation {
        let obj = Object.assign(new NormalizationEngineStatementPushEquipmentLocation(), model);
        NormalizationEngineStatementPushEquipmentLocation.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationEngineStatementPushEquipmentLocation>): NormalizationEngineStatementPushEquipmentLocation {
        if (!model) return null;
        return NormalizationEngineStatementPushEquipmentLocation.newInstance(<NormalizationEngineStatementPushEquipmentLocation> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationEngineStatementPushEquipmentLocation) {
        models.EngineStatementFromNormalization.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.equipment) {
            models.EngineBlock.fixupPrototype(this.equipment);
        }
        if (this.value) {
            models.EngineBlock.fixupPrototype(this.value);
        }
    }

    equipment: models.EngineExpression;

    value: models.EngineExpression;

    type: models.LocationType;

}
