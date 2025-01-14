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

export class PaneFieldAggregatedValue extends models.PaneField {
    constructor() {
        super();
        this.setDiscriminator("PaneFieldAggregatedValue");
    }

    getFixupPrototypeFunction() { return PaneFieldAggregatedValue.fixupPrototype; }

    static newInstance(model: Partial<PaneFieldAggregatedValue>): PaneFieldAggregatedValue {
        let obj = Object.assign(new PaneFieldAggregatedValue(), model);
        PaneFieldAggregatedValue.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<PaneFieldAggregatedValue>): PaneFieldAggregatedValue {
        if (!model) return null;
        return PaneFieldAggregatedValue.newInstance(<PaneFieldAggregatedValue> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: PaneFieldAggregatedValue) {
        models.PaneField.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.value) {
            models.ControlPointsGroup.fixupPrototype(this.value);
        }
    }

    value: models.ControlPointsGroup;

}
