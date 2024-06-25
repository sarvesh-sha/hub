/**
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Waypoint APIs
 * APIs and Definitions for the Optio3 Waypoint product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

import * as models from './models';

export class SensorResultForMontageBluetoothGateway extends models.SensorResult {
    constructor() {
        super();
        this.setDiscriminator("SensorResultForMontageBluetoothGateway");
    }

    getFixupPrototypeFunction() { return SensorResultForMontageBluetoothGateway.fixupPrototype; }

    static newInstance(model: Partial<SensorResultForMontageBluetoothGateway>): SensorResultForMontageBluetoothGateway {
        let obj = Object.assign(new SensorResultForMontageBluetoothGateway(), model);
        SensorResultForMontageBluetoothGateway.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<SensorResultForMontageBluetoothGateway>): SensorResultForMontageBluetoothGateway {
        if (!model) return null;
        return SensorResultForMontageBluetoothGateway.newInstance(<SensorResultForMontageBluetoothGateway> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: SensorResultForMontageBluetoothGateway) {
        models.SensorResult.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
    }

    detectedHeartbeat: boolean;

    detectedPixelTag: boolean;

    detectedSmartLock: boolean;

    detectedTRH: boolean;

}