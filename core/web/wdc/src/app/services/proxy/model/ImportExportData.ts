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

export class ImportExportData {
    __type: string;

    protected setDiscriminator(type: string) {
        this.__type = type;
    }

    getFixupPrototypeFunction() { return ImportExportData.fixupPrototype; }

    static newInstance(model: Partial<ImportExportData>): ImportExportData {
        let obj = Object.assign(new ImportExportData(), model);
        ImportExportData.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ImportExportData>): ImportExportData {
        if (!model) return null;
        return ImportExportData.newInstance(<ImportExportData> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ImportExportData) {
        if (!obj) return;

        switch (obj.__type)
        {
            case "BACnetBulkRenamingData":
                Object.setPrototypeOf(obj, models.BACnetBulkRenamingData.prototype);
                break;
            case "BACnetImportExportData":
                Object.setPrototypeOf(obj, models.BACnetImportExportData.prototype);
                break;
        }

        obj.fixupFields();
    }

    fixupFields() {
    }

    sysId: string;

    deviceName: string;

    deviceDescription: string;

    deviceLocation: string;

    deviceStructure: Array<string>;

    deviceVendor: string;

    deviceModel: string;

    dashboardName: string;

    dashboardEquipmentName: string;

    dashboardStructure: Array<string>;

    normalizedName: string;

    isSampled: boolean;

    pointClassId: string;

    pointClassAdt: string;

    pointTags: Array<string>;

    locationName: string;

    locationSysId: string;

    units: models.EngineeringUnits;

    value: string;

}
