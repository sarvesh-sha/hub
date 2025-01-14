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

export class ScatterPlot {
    getFixupPrototypeFunction() { return ScatterPlot.fixupPrototype; }

    static newInstance(model: Partial<ScatterPlot>): ScatterPlot {
        let obj = Object.assign(new ScatterPlot(), model);
        ScatterPlot.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ScatterPlot>): ScatterPlot {
        if (!model) return null;
        return ScatterPlot.newInstance(<ScatterPlot> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ScatterPlot) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.ScatterPlot.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.sourceTuples) {
            for (let val of this.sourceTuples) {
                models.ScatterPlotSourceTuple.fixupPrototype(val);
            }
        }
    }

    sourceTuples: Array<models.ScatterPlotSourceTuple>;

}
