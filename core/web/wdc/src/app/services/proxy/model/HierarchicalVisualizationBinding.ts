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

export class HierarchicalVisualizationBinding {
    getFixupPrototypeFunction() { return HierarchicalVisualizationBinding.fixupPrototype; }

    static newInstance(model: Partial<HierarchicalVisualizationBinding>): HierarchicalVisualizationBinding {
        let obj = Object.assign(new HierarchicalVisualizationBinding(), model);
        HierarchicalVisualizationBinding.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<HierarchicalVisualizationBinding>): HierarchicalVisualizationBinding {
        if (!model) return null;
        return HierarchicalVisualizationBinding.newInstance(<HierarchicalVisualizationBinding> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: HierarchicalVisualizationBinding) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.HierarchicalVisualizationBinding.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.options) {
            models.HierarchicalVisualizationConfiguration.fixupPrototype(this.options);
        }
        if (this.color) {
            models.ColorConfiguration.fixupPrototype(this.color);
        }
    }

    leafNodeId: string;

    options: models.HierarchicalVisualizationConfiguration;

    color: models.ColorConfiguration;

}