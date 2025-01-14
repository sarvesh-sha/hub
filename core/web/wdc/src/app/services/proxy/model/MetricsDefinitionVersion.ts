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

export class MetricsDefinitionVersion {
    static readonly RECORD_IDENTITY: string = "MetricsDefinitionVersion";

    getFixupPrototypeFunction() { return MetricsDefinitionVersion.fixupPrototype; }

    static newInstance(model: Partial<MetricsDefinitionVersion>): MetricsDefinitionVersion {
        let obj = Object.assign(new MetricsDefinitionVersion(), model);
        MetricsDefinitionVersion.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<MetricsDefinitionVersion>): MetricsDefinitionVersion {
        if (!model) return null;
        return MetricsDefinitionVersion.newInstance(<MetricsDefinitionVersion> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: MetricsDefinitionVersion) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.MetricsDefinitionVersion.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.version === undefined) {
            this.version = 0;
        }
        if (typeof this.createdOn === "string") {
            this.createdOn = new Date(<string><any>this.createdOn);
        }
        if (typeof this.updatedOn === "string") {
            this.updatedOn = new Date(<string><any>this.updatedOn);
        }
        if (this.definition) {
            models.RecordIdentity.fixupPrototype(this.definition);
        }
        if (this.details) {
            models.MetricsDefinitionDetails.fixupPrototype(this.details);
        }
        if (this.predecessor) {
            models.RecordIdentity.fixupPrototype(this.predecessor);
        }
        if (this.successors) {
            for (let val of this.successors) {
                models.RecordIdentity.fixupPrototype(val);
            }
        }
    }

    sysId: string;

    createdOn: Date;

    updatedOn: Date;

    version: number;

    definition: models.RecordIdentity;

    details: models.MetricsDefinitionDetails;

    predecessor: models.RecordIdentity;

    successors: Array<models.RecordIdentity>;

}
