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

export class UserGroupImportExport {
    getFixupPrototypeFunction() { return UserGroupImportExport.fixupPrototype; }

    static newInstance(model: Partial<UserGroupImportExport>): UserGroupImportExport {
        let obj = Object.assign(new UserGroupImportExport(), model);
        UserGroupImportExport.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<UserGroupImportExport>): UserGroupImportExport {
        if (!model) return null;
        return UserGroupImportExport.newInstance(<UserGroupImportExport> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: UserGroupImportExport) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.UserGroupImportExport.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.roles) {
            for (let val of this.roles) {
                models.Role.fixupPrototype(val);
            }
        }
        if (this.groups) {
            for (let val of this.groups) {
                models.UserGroup.fixupPrototype(val);
            }
        }
    }

    roles: Array<models.Role>;

    groups: Array<models.UserGroup>;

}