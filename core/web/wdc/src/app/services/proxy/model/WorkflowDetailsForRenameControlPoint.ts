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

export class WorkflowDetailsForRenameControlPoint extends models.WorkflowDetails {
    constructor() {
        super();
        this.setDiscriminator("WorkflowDetailsForRenameControlPoint");
    }

    getFixupPrototypeFunction() { return WorkflowDetailsForRenameControlPoint.fixupPrototype; }

    static newInstance(model: Partial<WorkflowDetailsForRenameControlPoint>): WorkflowDetailsForRenameControlPoint {
        let obj = Object.assign(new WorkflowDetailsForRenameControlPoint(), model);
        WorkflowDetailsForRenameControlPoint.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<WorkflowDetailsForRenameControlPoint>): WorkflowDetailsForRenameControlPoint {
        if (!model) return null;
        return WorkflowDetailsForRenameControlPoint.newInstance(<WorkflowDetailsForRenameControlPoint> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: WorkflowDetailsForRenameControlPoint) {
        models.WorkflowDetails.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    controlPoints: Array<string>;

    controlPointNewName: string;

}
