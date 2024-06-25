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

export class AlertEngineValueTravelLog extends models.EngineValueListConcrete {
    constructor() {
        super();
        this.setDiscriminator("AlertEngineValueTravelLog");
    }

    getFixupPrototypeFunction() { return AlertEngineValueTravelLog.fixupPrototype; }

    static newInstance(model: Partial<AlertEngineValueTravelLog>): AlertEngineValueTravelLog {
        let obj = Object.assign(new AlertEngineValueTravelLog(), model);
        AlertEngineValueTravelLog.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<AlertEngineValueTravelLog>): AlertEngineValueTravelLog {
        if (!model) return null;
        return AlertEngineValueTravelLog.newInstance(<AlertEngineValueTravelLog> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: AlertEngineValueTravelLog) {
        models.EngineValueListConcrete.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.elements) {
            for (let val of this.elements) {
                models.EngineValue.fixupPrototype(val);
            }
        }
    }

    elements: Array<models.AlertEngineValueTravelEntry>;

}