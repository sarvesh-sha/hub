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

export class DigineousPointLibrary {
    getFixupPrototypeFunction() { return DigineousPointLibrary.fixupPrototype; }

    static newInstance(model: Partial<DigineousPointLibrary>): DigineousPointLibrary {
        let obj = Object.assign(new DigineousPointLibrary(), model);
        DigineousPointLibrary.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DigineousPointLibrary>): DigineousPointLibrary {
        if (!model) return null;
        return DigineousPointLibrary.newInstance(<DigineousPointLibrary> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DigineousPointLibrary) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DigineousPointLibrary.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.lowInputRange === undefined) {
            this.lowInputRange = 0;
        }
        if (this.highInputRange === undefined) {
            this.highInputRange = 0;
        }
        if (this.lowOutputRange === undefined) {
            this.lowOutputRange = 0;
        }
        if (this.highOutputRange === undefined) {
            this.highOutputRange = 0;
        }
        if (this.pointClass) {
            models.WellKnownPointClassOrCustom.fixupPrototype(this.pointClass);
        }
    }

    identifier: string;

    description: string;

    pointClass: models.WellKnownPointClassOrCustom;

    tags: Array<string>;

    units: models.EngineeringUnits;

    lowInputRange: number;

    highInputRange: number;

    lowOutputRange: number;

    highOutputRange: number;

    enabled: boolean;

}
