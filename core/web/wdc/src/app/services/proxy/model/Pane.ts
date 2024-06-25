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

export class Pane {
    getFixupPrototypeFunction() { return Pane.fixupPrototype; }

    static newInstance(model: Partial<Pane>): Pane {
        let obj = Object.assign(new Pane(), model);
        Pane.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<Pane>): Pane {
        if (!model) return null;
        return Pane.newInstance(<Pane> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: Pane) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.Pane.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.branding) {
            models.BrandingConfiguration.fixupPrototype(this.branding);
        }
        if (this.cards) {
            for (let val of this.cards) {
                models.PaneCard.fixupPrototype(val);
            }
        }
    }

    title: string;

    branding: models.BrandingConfiguration;

    cards: Array<models.PaneCard>;

}
