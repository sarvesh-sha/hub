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

export class ProtocolConfigForBACnet extends models.ProtocolConfig {
    constructor() {
        super();
        this.setDiscriminator("ProtocolConfigForBACnet");
    }

    getFixupPrototypeFunction() { return ProtocolConfigForBACnet.fixupPrototype; }

    static newInstance(model: Partial<ProtocolConfigForBACnet>): ProtocolConfigForBACnet {
        let obj = Object.assign(new ProtocolConfigForBACnet(), model);
        ProtocolConfigForBACnet.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<ProtocolConfigForBACnet>): ProtocolConfigForBACnet {
        if (!model) return null;
        return ProtocolConfigForBACnet.newInstance(<ProtocolConfigForBACnet> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: ProtocolConfigForBACnet) {
        models.ProtocolConfig.fixupPrototype(obj);
    }

    fixupFields() {
        super.fixupFields();
        if (this.networkPort === undefined) {
            this.networkPort = 0;
        }
        if (this.maxParallelRequestsPerHost === undefined) {
            this.maxParallelRequestsPerHost = 0;
        }
        if (this.maxParallelRequestsPerNetwork === undefined) {
            this.maxParallelRequestsPerNetwork = 0;
        }
        if (this.limitPacketRate === undefined) {
            this.limitPacketRate = 0;
        }
        if (this.limitScan) {
            models.WhoIsRange.fixupPrototype(this.limitScan);
        }
        if (this.bbmds) {
            for (let val of this.bbmds) {
                models.BACnetBBMD.fixupPrototype(val);
            }
        }
        if (this.scanSubnets) {
            for (let val of this.scanSubnets) {
                models.FilteredSubnet.fixupPrototype(val);
            }
        }
        if (this.nonDiscoverableDevices) {
            for (let val of this.nonDiscoverableDevices) {
                models.NonDiscoverableBACnetDevice.fixupPrototype(val);
            }
        }
        if (this.nonDiscoverableMstpTrunks) {
            for (let val of this.nonDiscoverableMstpTrunks) {
                models.NonDiscoverableMstpTrunk.fixupPrototype(val);
            }
        }
        if (this.skippedDevices) {
            for (let val of this.skippedDevices) {
                models.SkippedBACnetDevice.fixupPrototype(val);
            }
        }
        if (this.filterSubnets) {
            for (let val of this.filterSubnets) {
                models.FilteredSubnet.fixupPrototype(val);
            }
        }
    }

    useUDP: boolean;

    useEthernet: boolean;

    disableBroadcast: boolean;

    sweepSubnet: boolean;

    sweepMSTP: boolean;

    includeNetworksFromRouters: boolean;

    networkPort: number;

    bbmds: Array<models.BACnetBBMD>;

    scanSubnets: Array<models.FilteredSubnet>;

    limitScan: models.WhoIsRange;

    maxParallelRequestsPerHost: number;

    maxParallelRequestsPerNetwork: number;

    limitPacketRate: number;

    nonDiscoverableDevices: Array<models.NonDiscoverableBACnetDevice>;

    nonDiscoverableMstpTrunks: Array<models.NonDiscoverableMstpTrunk>;

    skippedDevices: Array<models.SkippedBACnetDevice>;

    filterSubnets: Array<models.FilteredSubnet>;

}