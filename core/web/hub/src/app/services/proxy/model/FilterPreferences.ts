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

export class FilterPreferences {
    getFixupPrototypeFunction() { return FilterPreferences.fixupPrototype; }

    static newInstance(model: Partial<FilterPreferences>): FilterPreferences {
        let obj = Object.assign(new FilterPreferences(), model);
        FilterPreferences.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<FilterPreferences>): FilterPreferences {
        if (!model) return null;
        return FilterPreferences.newInstance(<FilterPreferences> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: FilterPreferences) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.FilterPreferences.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.alertRules) {
            for (let val of this.alertRules) {
                models.RecordIdentity.fixupPrototype(val);
            }
        }
    }

    id: string;

    name: string;

    locationIDs: Array<string>;

    alertRules: Array<models.RecordIdentity>;

    alertStatusIDs: Array<models.AlertStatus>;

    alertSeverityIDs: Array<models.AlertSeverity>;

    alertTypeIDs: Array<models.AlertType>;

    likeDeviceManufacturerName: string;

    likeDeviceProductName: string;

    likeDeviceModelName: string;

    equipmentIDs: Array<string>;

    equipmentClassIDs: Array<string>;

    deviceIDs: Array<string>;

    pointClassIDs: Array<string>;

    isSampling: models.FilterPreferenceBoolean;

    isClassified: models.FilterPreferenceBoolean;

    assignedToIDs: Array<string>;

    createdByIDs: Array<string>;

    workflowTypeIDs: Array<models.WorkflowType>;

    workflowStatusIDs: Array<models.WorkflowStatus>;

    workflowPriorityIDs: Array<models.WorkflowPriority>;

}