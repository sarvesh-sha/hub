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

export class ClassificationPointOutput {
    getFixupPrototypeFunction() { return ClassificationPointOutput.fixupPrototype; }

    static newInstance(model: Partial<ClassificationPointOutput>): ClassificationPointOutput {
        let obj = Object.assign(new ClassificationPointOutput(), model);
        ClassificationPointOutput.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ClassificationPointOutput>): ClassificationPointOutput {
        if (!model) return null;
        return ClassificationPointOutput.newInstance(<ClassificationPointOutput> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ClassificationPointOutput) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.ClassificationPointOutput.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.details) {
            models.ClassificationPointInputDetails.fixupPrototype(this.details);
        }
        if (this.lastResult) {
            models.ClassificationPointOutputDetails.fixupPrototype(this.lastResult);
        }
        if (this.currentResult) {
            models.ClassificationPointOutputDetails.fixupPrototype(this.currentResult);
        }
        if (this.equipmentOverrides) {
            for (let val of this.equipmentOverrides) {
                models.NormalizationEquipment.fixupPrototype(val);
            }
        }
        if (this.normalizationHistory) {
            for (let val of this.normalizationHistory) {
                models.NormalizationMatchHistory.fixupPrototype(val);
            }
        }
        if (this.locations) {
            for (let val of this.locations) {
                models.NormalizationEquipmentLocation.fixupPrototype(val);
            }
        }
        if (this.equipments) {
            for (let key in this.equipments) {
                let val = this.equipments[key];
                if (val) {
                    models.NormalizationEquipment.fixupPrototype(val);
                }
            }
        }
    }

    sysId: string;

    parentSysId: string;

    networkSysId: string;

    pointClassOverride: string;

    equipmentOverrides: Array<models.NormalizationEquipment>;

    details: models.ClassificationPointInputDetails;

    normalizedName: string;

    oldNormalizedName: string;

    normalizationHistory: Array<models.NormalizationMatchHistory>;

    lastResult: models.ClassificationPointOutputDetails;

    currentResult: models.ClassificationPointOutputDetails;

    equipments: { [key: string]: models.NormalizationEquipment; };

    equipmentRelationships: { [key: string]: Array<string>; };

    locations: Array<models.NormalizationEquipmentLocation>;

    matchingDimensions: Array<string>;

    normalizationTags: Array<string>;

}
