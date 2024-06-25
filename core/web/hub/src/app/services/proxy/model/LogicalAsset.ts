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

export class LogicalAsset extends models.Asset {
    constructor() {
        super();
        this.setDiscriminator("LogicalAsset");
    }

    static readonly RECORD_IDENTITY: string = "LogicalAsset";

    getFixupPrototypeFunction() { return LogicalAsset.fixupPrototype; }

    static newInstance(model: Partial<LogicalAsset>): LogicalAsset {
        let obj = Object.assign(new LogicalAsset(), model);
        LogicalAsset.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<LogicalAsset>): LogicalAsset {
        if (!model) return null;
        return LogicalAsset.newInstance(<LogicalAsset> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: LogicalAsset) {
        models.Asset.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}