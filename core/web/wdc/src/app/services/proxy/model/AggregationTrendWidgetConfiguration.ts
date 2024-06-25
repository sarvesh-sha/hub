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

export class AggregationTrendWidgetConfiguration extends models.WidgetConfiguration {
    constructor() {
        super();
        this.setDiscriminator("AggregationTrendWidgetConfiguration");
    }

    getFixupPrototypeFunction() { return AggregationTrendWidgetConfiguration.fixupPrototype; }

    static newInstance(model: Partial<AggregationTrendWidgetConfiguration>): AggregationTrendWidgetConfiguration {
        let obj = Object.assign(new AggregationTrendWidgetConfiguration(), model);
        AggregationTrendWidgetConfiguration.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AggregationTrendWidgetConfiguration>): AggregationTrendWidgetConfiguration {
        if (!model) return null;
        return AggregationTrendWidgetConfiguration.newInstance(<AggregationTrendWidgetConfiguration> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AggregationTrendWidgetConfiguration) {
        models.WidgetConfiguration.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.filterableRange) {
            models.FilterableTimeRange.fixupPrototype(this.filterableRange);
        }
        if (this.groups) {
            for (let val of this.groups) {
                models.ControlPointsGroup.fixupPrototype(val);
            }
        }
    }

    groups: Array<models.ControlPointsGroup>;

    filterableRange: models.FilterableTimeRange;

    granularity: models.AggregationGranularity;

    visualizationMode: models.AggregationTrendVisualizationMode;

    showY: boolean;

    showLegend: boolean;

}