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

export class UserMessageGeneric extends models.UserMessage {
    constructor() {
        super();
        this.setDiscriminator("UserMessageGeneric");
    }

    static readonly RECORD_IDENTITY: string = "UserMessageGeneric";

    getFixupPrototypeFunction() { return UserMessageGeneric.fixupPrototype; }

    static newInstance(model: Partial<UserMessageGeneric>): UserMessageGeneric {
        let obj = Object.assign(new UserMessageGeneric(), model);
        UserMessageGeneric.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<UserMessageGeneric>): UserMessageGeneric {
        if (!model) return null;
        return UserMessageGeneric.newInstance(<UserMessageGeneric> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: UserMessageGeneric) {
        models.UserMessage.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

}