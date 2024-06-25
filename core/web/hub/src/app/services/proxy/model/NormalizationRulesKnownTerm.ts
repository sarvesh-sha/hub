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

export class NormalizationRulesKnownTerm {
    getFixupPrototypeFunction() { return NormalizationRulesKnownTerm.fixupPrototype; }

    static newInstance(model: Partial<NormalizationRulesKnownTerm>): NormalizationRulesKnownTerm {
        let obj = Object.assign(new NormalizationRulesKnownTerm(), model);
        NormalizationRulesKnownTerm.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationRulesKnownTerm>): NormalizationRulesKnownTerm {
        if (!model) return null;
        return NormalizationRulesKnownTerm.newInstance(<NormalizationRulesKnownTerm> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationRulesKnownTerm) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.NormalizationRulesKnownTerm.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.positiveWeight === undefined) {
            this.positiveWeight = 0;
        }
        if (this.negativeWeight === undefined) {
            this.negativeWeight = 0;
        }
    }

    acronym: string;

    positiveWeight: number;

    negativeWeight: number;

    weightReason: string;

    synonyms: Array<string>;

}
