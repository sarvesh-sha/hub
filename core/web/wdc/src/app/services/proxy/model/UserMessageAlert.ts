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

export class UserMessageAlert extends models.UserMessage {
    constructor() {
        super();
        this.setDiscriminator("UserMessageAlert");
    }

    static readonly RECORD_IDENTITY: string = "UserMessageAlert";

    getFixupPrototypeFunction() { return UserMessageAlert.fixupPrototype; }

    static newInstance(model: Partial<UserMessageAlert>): UserMessageAlert {
        let obj = Object.assign(new UserMessageAlert(), model);
        UserMessageAlert.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<UserMessageAlert>): UserMessageAlert {
        if (!model) return null;
        return UserMessageAlert.newInstance(<UserMessageAlert> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: UserMessageAlert) {
        models.UserMessage.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.alert) {
            models.RecordIdentity.fixupPrototype(this.alert);
        }
    }

    alert: models.RecordIdentity;

}