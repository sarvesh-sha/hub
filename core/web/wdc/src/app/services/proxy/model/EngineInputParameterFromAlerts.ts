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

export class EngineInputParameterFromAlerts extends models.EngineInputParameter {
    getFixupPrototypeFunction() { return EngineInputParameterFromAlerts.fixupPrototype; }

    static newInstance(model: Partial<EngineInputParameterFromAlerts>): EngineInputParameterFromAlerts {
        let obj = Object.assign(new EngineInputParameterFromAlerts(), model);
        EngineInputParameterFromAlerts.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<EngineInputParameterFromAlerts>): EngineInputParameterFromAlerts {
        if (!model) return null;
        return EngineInputParameterFromAlerts.newInstance(<EngineInputParameterFromAlerts> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: EngineInputParameterFromAlerts) {
        models.EngineInputParameter.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}
