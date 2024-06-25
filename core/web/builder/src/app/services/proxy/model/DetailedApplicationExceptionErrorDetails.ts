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

export class DetailedApplicationExceptionErrorDetails {
    getFixupPrototypeFunction() { return DetailedApplicationExceptionErrorDetails.fixupPrototype; }

    static newInstance(model: Partial<DetailedApplicationExceptionErrorDetails>): DetailedApplicationExceptionErrorDetails {
        let obj = Object.assign(new DetailedApplicationExceptionErrorDetails(), model);
        DetailedApplicationExceptionErrorDetails.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DetailedApplicationExceptionErrorDetails>): DetailedApplicationExceptionErrorDetails {
        if (!model) return null;
        return DetailedApplicationExceptionErrorDetails.newInstance(<DetailedApplicationExceptionErrorDetails> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DetailedApplicationExceptionErrorDetails) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DetailedApplicationExceptionErrorDetails.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.validationErrors) {
            models.ValidationResults.fixupPrototype(this.validationErrors);
        }
    }

    code: models.DetailedApplicationExceptionCode;

    message: string;

    exceptionTrace: string;

    validationErrors: models.ValidationResults;

}
