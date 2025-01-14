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

export class ControlPointWidgetConfiguration extends models.WidgetConfiguration {
    constructor() {
        super();
        this.setDiscriminator("ControlPointWidgetConfiguration");
    }

    getFixupPrototypeFunction() { return ControlPointWidgetConfiguration.fixupPrototype; }

    static newInstance(model: Partial<ControlPointWidgetConfiguration>): ControlPointWidgetConfiguration {
        let obj = Object.assign(new ControlPointWidgetConfiguration(), model);
        ControlPointWidgetConfiguration.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ControlPointWidgetConfiguration>): ControlPointWidgetConfiguration {
        if (!model) return null;
        return ControlPointWidgetConfiguration.newInstance(<ControlPointWidgetConfiguration> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ControlPointWidgetConfiguration) {
        models.WidgetConfiguration.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.valuePrecision === undefined) {
            this.valuePrecision = 0;
        }
        if (this.pointInput) {
            models.AssetGraphBinding.fixupPrototype(this.pointInput);
        }
        if (this.valueUnits) {
            models.EngineeringUnitsFactors.fixupPrototype(this.valueUnits);
        }
        if (this.color) {
            models.ColorConfiguration.fixupPrototype(this.color);
        }
    }

    pointId: string;

    pointInput: models.AssetGraphBinding;

    nameEnabled: boolean;

    nameDisplay: models.ControlPointDisplayType;

    nameAlignment: models.HorizontalAlignment;

    valueEnabled: boolean;

    valueUnits: models.EngineeringUnitsFactors;

    valueUnitsEnabled: boolean;

    valuePrecision: number;

    valueAlignment: models.HorizontalAlignment;

    timestampEnabled: boolean;

    timestampFormat: string;

    timestampAlignment: models.HorizontalAlignment;

    color: models.ColorConfiguration;

}
