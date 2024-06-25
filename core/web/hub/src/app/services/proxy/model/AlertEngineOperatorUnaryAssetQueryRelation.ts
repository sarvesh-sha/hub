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

export class AlertEngineOperatorUnaryAssetQueryRelation extends models.EngineOperatorUnaryFromAlerts {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineOperatorUnaryAssetQueryRelation");
    }

    getFixupPrototypeFunction() { return AlertEngineOperatorUnaryAssetQueryRelation.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineOperatorUnaryAssetQueryRelation>): AlertEngineOperatorUnaryAssetQueryRelation {
        let obj = Object.assign(new AlertEngineOperatorUnaryAssetQueryRelation(), model);
        AlertEngineOperatorUnaryAssetQueryRelation.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineOperatorUnaryAssetQueryRelation>): AlertEngineOperatorUnaryAssetQueryRelation {
        if (!model) return null;
        return AlertEngineOperatorUnaryAssetQueryRelation.newInstance(<AlertEngineOperatorUnaryAssetQueryRelation> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineOperatorUnaryAssetQueryRelation) {
        models.EngineOperatorUnaryFromAlerts.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    relation: models.AssetRelationship;

    fromChild: boolean;

}