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

export class ScatterPlotSourceTuple {
    getFixupPrototypeFunction() { return ScatterPlotSourceTuple.fixupPrototype; }

    static newInstance(model: Partial<ScatterPlotSourceTuple>): ScatterPlotSourceTuple {
        let obj = Object.assign(new ScatterPlotSourceTuple(), model);
        ScatterPlotSourceTuple.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ScatterPlotSourceTuple>): ScatterPlotSourceTuple {
        if (!model) return null;
        return ScatterPlotSourceTuple.newInstance(<ScatterPlotSourceTuple> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ScatterPlotSourceTuple) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.ScatterPlotSourceTuple.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.panel === undefined) {
            this.panel = 0;
        }
        if (this.sourceX) {
            models.ScatterPlotSource.fixupPrototype(this.sourceX);
        }
        if (this.sourceY) {
            models.ScatterPlotSource.fixupPrototype(this.sourceY);
        }
        if (this.sourceZ) {
            models.ScatterPlotSource.fixupPrototype(this.sourceZ);
        }
    }

    sourceX: models.ScatterPlotSource;

    sourceY: models.ScatterPlotSource;

    sourceZ: models.ScatterPlotSource;

    name: string;

    colorOverride: string;

    panel: number;

}
