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

export class MetricsDefinitionDetails {
    __type: string;

    protected setDiscriminator(type: string) {
        this.__type = type;
    }

    getFixupPrototypeFunction() { return MetricsDefinitionDetails.fixupPrototype; }

    static newInstance(model: Partial<MetricsDefinitionDetails>): MetricsDefinitionDetails {
        let obj = Object.assign(new MetricsDefinitionDetails(), model);
        MetricsDefinitionDetails.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MetricsDefinitionDetails>): MetricsDefinitionDetails {
        if (!model) return null;
        return MetricsDefinitionDetails.newInstance(<MetricsDefinitionDetails> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MetricsDefinitionDetails) {
        if (!obj) return;

        switch (obj.__type)
        {
            case "MetricsDefinitionDetailsForUserProgram":
                Object.setPrototypeOf(obj, models.MetricsDefinitionDetailsForUserProgram.prototype);
                break;
        }

        obj.fixupFields();
    }

    fixupFields() {
        if (this.graph) {
            models.AssetGraph.fixupPrototype(this.graph);
        }
        if (this.tabs) {
            for (let val of this.tabs) {
                models.EngineTab.fixupPrototype(val);
            }
        }
    }

    tabs: Array<models.EngineTab>;

    temporary: boolean;

    graph: models.AssetGraph;

}
