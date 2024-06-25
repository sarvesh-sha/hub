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

export class MetricsBinding {
    getFixupPrototypeFunction() { return MetricsBinding.fixupPrototype; }

    static newInstance(model: Partial<MetricsBinding>): MetricsBinding {
        let obj = Object.assign(new MetricsBinding(), model);
        MetricsBinding.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MetricsBinding>): MetricsBinding {
        if (!model) return null;
        return MetricsBinding.newInstance(<MetricsBinding> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MetricsBinding) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.MetricsBinding.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.schema) {
            models.TimeSeriesPropertyType.fixupPrototype(this.schema);
        }
        if (this.bindingForSeries) {
            for (let key in this.bindingForSeries) {
                let val = this.bindingForSeries[key];
                if (val) {
                    models.MetricsBindingForSeries.fixupPrototype(val);
                }
            }
        }
        if (this.bindingForSetOfSeries) {
            for (let key in this.bindingForSetOfSeries) {
                let val = this.bindingForSetOfSeries[key];
                if (val) {
                    models.MetricsBindingForSetOfSeries.fixupPrototype(val);
                }
            }
        }
    }

    detailsHash: string;

    namedOutput: string;

    schema: models.TimeSeriesPropertyType;

    bindingForSeries: { [key: string]: models.MetricsBindingForSeries; };

    bindingForSetOfSeries: { [key: string]: models.MetricsBindingForSetOfSeries; };

}
