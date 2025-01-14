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

export class DeploymentHostStatusDescriptor {
    getFixupPrototypeFunction() { return DeploymentHostStatusDescriptor.fixupPrototype; }

    static newInstance(model: Partial<DeploymentHostStatusDescriptor>): DeploymentHostStatusDescriptor {
        let obj = Object.assign(new DeploymentHostStatusDescriptor(), model);
        DeploymentHostStatusDescriptor.fixupPrototype(obj);
        return obj;
    }

    static deepClone(model: Partial<DeploymentHostStatusDescriptor>): DeploymentHostStatusDescriptor {
        if (!model) return null;
        return DeploymentHostStatusDescriptor.newInstance(<DeploymentHostStatusDescriptor> JSON.parse(JSON.stringify(model)));
    }

    static fixupPrototype(obj: DeploymentHostStatusDescriptor) {
        if (!obj) return;

        Object.setPrototypeOf(obj, models.DeploymentHostStatusDescriptor.prototype);

        obj.fixupFields();
    }

    fixupFields() {
        if (this.batteryVoltage === undefined) {
            this.batteryVoltage = 0;
        }
        if (this.cpuTemperature === undefined) {
            this.cpuTemperature = 0;
        }
        if (this.diskTotal === undefined) {
            this.diskTotal = 0;
        }
        if (this.diskFree === undefined) {
            this.diskFree = 0;
        }
        if (typeof this.createdOn === "string") {
            this.createdOn = new Date(<string><any>this.createdOn);
        }
        if (typeof this.lastHeartbeat === "string") {
            this.lastHeartbeat = new Date(<string><any>this.lastHeartbeat);
        }
        if (typeof this.agentBuildTime === "string") {
            this.agentBuildTime = new Date(<string><any>this.agentBuildTime);
        }
        if (this.ri) {
            models.RecordIdentity.fixupPrototype(this.ri);
        }
        if (this.hostDetails) {
            models.DeploymentHostDetails.fixupPrototype(this.hostDetails);
        }
        if (this.provisioningInfo) {
            models.DeploymentHostProvisioningInfo.fixupPrototype(this.provisioningInfo);
        }
        if (this.delayedOps) {
            for (let val of this.delayedOps) {
                models.DelayedOperation.fixupPrototype(val);
            }
        }
        if (this.tasks) {
            for (let key in this.tasks) {
                let val = this.tasks[key];
                if (val) {
                    models.DeploymentTask.fixupPrototype(val);
                }
            }
        }
        if (this.agents) {
            for (let key in this.agents) {
                let val = this.agents[key];
                if (val) {
                    models.DeploymentAgent.fixupPrototype(val);
                }
            }
        }
        if (this.images) {
            for (let key in this.images) {
                let val = this.images[key];
                if (val) {
                    models.RegistryImage.fixupPrototype(val);
                }
            }
        }
    }

    ri: models.RecordIdentity;

    customerSysId: string;

    customerName: string;

    serviceSysId: string;

    serviceName: string;

    serviceVertical: models.CustomerVertical;

    hostId: string;

    hostName: string;

    remoteName: string;

    roles: Array<models.DeploymentRole>;

    rolesSummary: string;

    tasks: { [key: string]: models.DeploymentTask; };

    agents: { [key: string]: models.DeploymentAgent; };

    images: { [key: string]: models.RegistryImage; };

    hostDetails: models.DeploymentHostDetails;

    provisioningInfo: models.DeploymentHostProvisioningInfo;

    createdOn: Date;

    lastHeartbeat: Date;

    agentBuildTime: Date;

    batteryVoltage: number;

    cpuTemperature: number;

    diskTotal: number;

    diskFree: number;

    instanceType: models.DeploymentInstance;

    architecture: models.DockerImageArchitecture;

    status: models.DeploymentStatus;

    operationalStatus: models.DeploymentOperationalStatus;

    responsiveness: models.DeploymentOperationalResponsiveness;

    delayedOps: Array<models.DelayedOperation>;

    flags: Array<models.DeploymentHostStatusDescriptorFlag>;

    preparedForCustomer: string;

    preparedForService: string;

}
