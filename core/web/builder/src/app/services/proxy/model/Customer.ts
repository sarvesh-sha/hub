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

export class Customer {
    static readonly RECORD_IDENTITY: string = "Customer";

    getFixupPrototypeFunction() { return Customer.fixupPrototype; }

    static newInstance(model: Partial<Customer>): Customer {
        let obj = Object.assign(new Customer(), model);
        Customer.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<Customer>): Customer {
        if (!model) return null;
        return Customer.newInstance(<Customer> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: Customer) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.Customer.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (typeof this.createdOn === "string") {
            this.createdOn = new Date(<string><any>this.createdOn);
        }
        if (typeof this.updatedOn === "string") {
            this.updatedOn = new Date(<string><any>this.updatedOn);
        }
        if (this.services) {
            for (let val of this.services) {
                models.RecordIdentity.fixupPrototype(val);
            }
        }
        if (this.sharedUsers) {
            for (let val of this.sharedUsers) {
                models.RecordIdentity.fixupPrototype(val);
            }
        }
        if (this.sharedSecrets) {
            for (let val of this.sharedSecrets) {
                models.RecordIdentity.fixupPrototype(val);
            }
        }
    }

    sysId: string;

    createdOn: Date;

    updatedOn: Date;

    cloudId: string;

    name: string;

    services: Array<models.RecordIdentity>;

    sharedUsers: Array<models.RecordIdentity>;

    sharedSecrets: Array<models.RecordIdentity>;

}