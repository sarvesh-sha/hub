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

export class RemoteCallDescriptor {
    getFixupPrototypeFunction() { return RemoteCallDescriptor.fixupPrototype; }

    static newInstance(model: Partial<RemoteCallDescriptor>): RemoteCallDescriptor {
        let obj = Object.assign(new RemoteCallDescriptor(), model);
        RemoteCallDescriptor.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<RemoteCallDescriptor>): RemoteCallDescriptor {
        if (!model) return null;
        return RemoteCallDescriptor.newInstance(<RemoteCallDescriptor> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: RemoteCallDescriptor) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.RemoteCallDescriptor.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.parameters) {
            for (let val of this.parameters) {
                models.RemoteArgument.fixupPrototype(val);
            }
        }
    }

    classId: string;

    methodName: string;

    parameters: Array<models.RemoteArgument>;

}
