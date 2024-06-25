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

export class Event {
    __type: string;

    protected setDiscriminator(type: string) {
        this.__type = type;
    }

    static readonly RECORD_IDENTITY: string = "Event";

    getFixupPrototypeFunction() { return Event.fixupPrototype; }

    static newInstance(model: Partial<Event>): Event {
        let obj = Object.assign(new Event(), model);
        Event.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<Event>): Event {
        if (!model) return null;
        return Event.newInstance(<Event> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: Event) {
        if (!obj) return;

        switch (obj.__type)
        {
            case "Alert":
                Object.setPrototypeOf(obj, models.Alert.prototype);
                break;
            case "Audit":
                Object.setPrototypeOf(obj, models.Audit.prototype);
                break;
            case "Workflow":
                Object.setPrototypeOf(obj, models.Workflow.prototype);
                break;
        }

        obj.fixupFields();
    }

    fixupFields() {
        if (this.sequenceNumber === undefined) {
            this.sequenceNumber = 0;
        }
        if (typeof this.createdOn === "string") {
            this.createdOn = new Date(<string><any>this.createdOn);
        }
        if (typeof this.updatedOn === "string") {
            this.updatedOn = new Date(<string><any>this.updatedOn);
        }
        if (this.asset) {
            models.RecordIdentity.fixupPrototype(this.asset);
        }
        if (this.location) {
            models.RecordIdentity.fixupPrototype(this.location);
        }
    }

    sysId: string;

    createdOn: Date;

    updatedOn: Date;

    asset: models.RecordIdentity;

    location: models.RecordIdentity;

    sequenceNumber: number;

    description: string;

    extendedDescription: string;

}
