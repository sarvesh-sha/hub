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

export class NormalizationEngineOperatorBinaryScoreDocuments extends models.EngineOperatorBinaryFromNormalization {
    constructor() {
        super();
        this.setDiscriminator("NormalizationEngineOperatorBinaryScoreDocuments");
    }

    getFixupPrototypeFunction() { return NormalizationEngineOperatorBinaryScoreDocuments.fixupPrototype; }

    static newInstance(model: Partial<NormalizationEngineOperatorBinaryScoreDocuments>): NormalizationEngineOperatorBinaryScoreDocuments {
        let obj = Object.assign(new NormalizationEngineOperatorBinaryScoreDocuments(), model);
        NormalizationEngineOperatorBinaryScoreDocuments.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationEngineOperatorBinaryScoreDocuments>): NormalizationEngineOperatorBinaryScoreDocuments {
        if (!model) return null;
        return NormalizationEngineOperatorBinaryScoreDocuments.newInstance(<NormalizationEngineOperatorBinaryScoreDocuments> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationEngineOperatorBinaryScoreDocuments) {
        models.EngineOperatorBinaryFromNormalization.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.minNgram === undefined) {
            this.minNgram = 0;
        }
        if (this.maxNgram === undefined) {
            this.maxNgram = 0;
        }
        if (this.minDocFrequency === undefined) {
            this.minDocFrequency = 0;
        }
        if (this.minScore === undefined) {
            this.minScore = 0;
        }
    }

    minNgram: number;

    maxNgram: number;

    minDocFrequency: number;

    minScore: number;

}
