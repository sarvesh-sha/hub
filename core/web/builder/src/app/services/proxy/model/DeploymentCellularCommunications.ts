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

export class DeploymentCellularCommunications {
    getFixupPrototypeFunction() { return DeploymentCellularCommunications.fixupPrototype; }

    static newInstance(model: Partial<DeploymentCellularCommunications>): DeploymentCellularCommunications {
        let obj = Object.assign(new DeploymentCellularCommunications(), model);
        DeploymentCellularCommunications.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DeploymentCellularCommunications>): DeploymentCellularCommunications {
        if (!model) return null;
        return DeploymentCellularCommunications.newInstance(<DeploymentCellularCommunications> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DeploymentCellularCommunications) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DeploymentCellularCommunications.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.sessions) {
            for (let key in this.sessions) {
                let val = this.sessions[key];
                if (val) {
                    models.DeploymentCellularCommunicationsDetails.fixupPrototype(val);
                }
            }
        }
    }

    sessions: { [key: string]: models.DeploymentCellularCommunicationsDetails; };

}
