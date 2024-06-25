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

export class NormalizationDefinitionDetails {
    __type: string;

    protected setDiscriminator(type: string) {
        this.__type = type;
    }

    getFixupPrototypeFunction() { return NormalizationDefinitionDetails.fixupPrototype; }

    static newInstance(model: Partial<NormalizationDefinitionDetails>): NormalizationDefinitionDetails {
        let obj = Object.assign(new NormalizationDefinitionDetails(), model);
        NormalizationDefinitionDetails.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<NormalizationDefinitionDetails>): NormalizationDefinitionDetails {
        if (!model) return null;
        return NormalizationDefinitionDetails.newInstance(<NormalizationDefinitionDetails> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: NormalizationDefinitionDetails) {
        if (!obj) return;

        switch (obj.__type)
        {
            case "NormalizationDefinitionDetailsForUserProgram":
                Object.setPrototypeOf(obj, models.NormalizationDefinitionDetailsForUserProgram.prototype);
                break;
        }

        obj.fixupFields();
    }

    fixupFields() {
        if (this.tabs) {
            for (let val of this.tabs) {
                models.EngineTab.fixupPrototype(val);
            }
        }
    }

    tabs: Array<models.EngineTab>;

    temporary: boolean;

}