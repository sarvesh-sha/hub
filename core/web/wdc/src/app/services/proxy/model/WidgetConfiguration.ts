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

export class WidgetConfiguration {
    __type: string;

    protected setDiscriminator(type: string) {
        this.__type = type;
    }

    getFixupPrototypeFunction() { return WidgetConfiguration.fixupPrototype; }

    static newInstance(model: Partial<WidgetConfiguration>): WidgetConfiguration {
        let obj = Object.assign(new WidgetConfiguration(), model);
        WidgetConfiguration.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<WidgetConfiguration>): WidgetConfiguration {
        if (!model) return null;
        return WidgetConfiguration.newInstance(<WidgetConfiguration> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: WidgetConfiguration) {
        if (!obj) return;

        switch (obj.__type)
        {
            case "AggregationTableWidgetConfiguration":
                Object.setPrototypeOf(obj, models.AggregationTableWidgetConfiguration.prototype);
                break;
            case "AggregationTrendWidgetConfiguration":
                Object.setPrototypeOf(obj, models.AggregationTrendWidgetConfiguration.prototype);
                break;
            case "AggregationWidgetConfiguration":
                Object.setPrototypeOf(obj, models.AggregationWidgetConfiguration.prototype);
                break;
            case "AlertFeedWidgetConfiguration":
                Object.setPrototypeOf(obj, models.AlertFeedWidgetConfiguration.prototype);
                break;
            case "AlertMapWidgetConfiguration":
                Object.setPrototypeOf(obj, models.AlertMapWidgetConfiguration.prototype);
                break;
            case "AlertSummaryWidgetConfiguration":
                Object.setPrototypeOf(obj, models.AlertSummaryWidgetConfiguration.prototype);
                break;
            case "AlertTableWidgetConfiguration":
                Object.setPrototypeOf(obj, models.AlertTableWidgetConfiguration.prototype);
                break;
            case "AlertTrendWidgetConfiguration":
                Object.setPrototypeOf(obj, models.AlertTrendWidgetConfiguration.prototype);
                break;
            case "AssetGraphSelectorWidgetConfiguration":
                Object.setPrototypeOf(obj, models.AssetGraphSelectorWidgetConfiguration.prototype);
                break;
            case "ControlPointWidgetConfiguration":
                Object.setPrototypeOf(obj, models.ControlPointWidgetConfiguration.prototype);
                break;
            case "DeviceSummaryWidgetConfiguration":
                Object.setPrototypeOf(obj, models.DeviceSummaryWidgetConfiguration.prototype);
                break;
            case "GroupingWidgetConfiguration":
                Object.setPrototypeOf(obj, models.GroupingWidgetConfiguration.prototype);
                break;
            case "ImageWidgetConfiguration":
                Object.setPrototypeOf(obj, models.ImageWidgetConfiguration.prototype);
                break;
            case "TextWidgetConfiguration":
                Object.setPrototypeOf(obj, models.TextWidgetConfiguration.prototype);
                break;
            case "TimeSeriesWidgetConfiguration":
                Object.setPrototypeOf(obj, models.TimeSeriesWidgetConfiguration.prototype);
                break;
        }

        obj.fixupFields();
    }

    fixupFields() {
        if (this.size === undefined) {
            this.size = 0;
        }
        if (this.refreshRateInSeconds === undefined) {
            this.refreshRateInSeconds = 0;
        }
        if (this.fontMultiplier === undefined) {
            this.fontMultiplier = 0;
        }
    }

    id: string;

    size: number;

    name: string;

    description: string;

    locations: Array<string>;

    refreshRateInSeconds: number;

    manualFontScaling: boolean;

    fontMultiplier: number;

    toolbarBehavior: models.WidgetToolbarBehavior;

}
