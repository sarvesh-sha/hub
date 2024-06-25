/**
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Builder APIs
 * APIs and Definitions for the Optio3 Builder product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

import * as models from './models';

export class CustomerServiceBackup {
    static readonly RECORD_IDENTITY: string = "CustomerServiceBackup";

    getFixupPrototypeFunction() { return CustomerServiceBackup.fixupPrototype; }

    static newInstance(model: Partial<CustomerServiceBackup>): CustomerServiceBackup {
        let obj = Object.assign(new CustomerServiceBackup(), model);
        CustomerServiceBackup.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<CustomerServiceBackup>): CustomerServiceBackup {
        if (!model) return null;
        return CustomerServiceBackup.newInstance(<CustomerServiceBackup> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: CustomerServiceBackup) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.CustomerServiceBackup.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.fileSize === undefined) {
            this.fileSize = 0;
        }
        if (typeof this.createdOn === "string") {
            this.createdOn = new Date(<string><any>this.createdOn);
        }
        if (typeof this.updatedOn === "string") {
            this.updatedOn = new Date(<string><any>this.updatedOn);
        }
        if (this.customerService) {
            models.RecordIdentity.fixupPrototype(this.customerService);
        }
        if (this.roleImages) {
            for (let val of this.roleImages) {
                models.RoleAndArchitectureWithImage.fixupPrototype(val);
            }
        }
    }

    sysId: string;

    createdOn: Date;

    updatedOn: Date;

    customerService: models.RecordIdentity;

    fileId: string;

    fileSize: number;

    fileIdOnAgent: string;

    pendingTransfer: boolean;

    trigger: models.BackupKind;

    extraConfigLines: string;

    roleImages: Array<models.RoleAndArchitectureWithImage>;

}
