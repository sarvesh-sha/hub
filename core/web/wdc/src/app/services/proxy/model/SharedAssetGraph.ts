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

export class SharedAssetGraph {
    getFixupPrototypeFunction() { return SharedAssetGraph.fixupPrototype; }

    static newInstance(model: Partial<SharedAssetGraph>): SharedAssetGraph {
        let obj = Object.assign(new SharedAssetGraph(), model);
        SharedAssetGraph.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<SharedAssetGraph>): SharedAssetGraph {
        if (!model) return null;
        return SharedAssetGraph.newInstance(<SharedAssetGraph> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: SharedAssetGraph) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.SharedAssetGraph.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.graph) {
            models.AssetGraph.fixupPrototype(this.graph);
        }
    }

    id: string;

    name: string;

    graph: models.AssetGraph;

}