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

export class Audit extends models.Event {
    constructor() {
        super();
        this.setDiscriminator("Audit");
    }

    static readonly RECORD_IDENTITY: string = "Audit";

    getFixupPrototypeFunction() { return Audit.fixupPrototype; }

    static newInstance(model: Partial<Audit>): Audit {
        let obj = Object.assign(new Audit(), model);
        Audit.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<Audit>): Audit {
        if (!model) return null;
        return Audit.newInstance(<Audit> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: Audit) {
        models.Event.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    type: models.AuditType;

}
