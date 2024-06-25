import {Component} from "@angular/core";

import {ProberDeviceDetails, ProberObjectDetails, ProberState} from "app/customer/data-collection/gateways/probers/probers-detail-page.component";

import * as Models from "app/services/proxy/model/models";
import {MACAddressDirective} from "framework/directives/mac-address.directive";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-gateway-probers-wizard-bacnet-config-step",
               templateUrl: "./probers-wizard-bacnet-config-step.component.html",
               providers  : [
                   WizardStep.createProvider(ProbersWizardBacnetConfigStep)
               ]
           })
export class ProbersWizardBacnetConfigStep extends WizardStep<ProberState>
{
    networksForMstpScan: MstpScanNetwork[] = [];

    limitScanLow: number;
    limitScanHigh: number;

    public get config(): Models.ProberOperationForBACnet
    {
        return <Models.ProberOperationForBACnet>this.data.operation;
    }

    public get configForDiscoverBBMDs(): Models.ProberOperationForBACnetToDiscoverBBMDs
    {
        if (this.data.operation instanceof Models.ProberOperationForBACnetToDiscoverBBMDs)
        {
            return this.data.operation;
        }

        return null;
    }

    public get configForDiscoverRouters(): Models.ProberOperationForBACnetToDiscoverRouters
    {
        if (this.data.operation instanceof Models.ProberOperationForBACnetToDiscoverRouters)
        {
            return this.data.operation;
        }

        return null;
    }

    public get configForDiscoverDevices(): Models.ProberOperationForBACnetToDiscoverDevices
    {
        if (this.data.operation instanceof Models.ProberOperationForBACnetToDiscoverDevices)
        {
            return this.data.operation;
        }

        return null;
    }

    public get configForScanSubnetForDevices(): Models.ProberOperationForBACnetToScanSubnetForDevices
    {
        if (this.data.operation instanceof Models.ProberOperationForBACnetToScanSubnetForDevices)
        {
            return this.data.operation;
        }

        return null;
    }

    public get configForScanMstpTrunkForDevices(): Models.ProberOperationForBACnetToScanMstpTrunkForDevices
    {
        if (this.data.operation instanceof Models.ProberOperationForBACnetToScanMstpTrunkForDevices)
        {
            return this.data.operation;
        }

        return null;
    }

    public get configForAutoDiscovery(): Models.ProberOperationForBACnetToAutoDiscovery
    {
        if (this.data.operation instanceof Models.ProberOperationForBACnetToAutoDiscovery)
        {
            return this.data.operation;
        }

        return null;
    }

    public get configForReadBBMDs(): Models.ProberOperationForBACnetToReadBBMDs
    {
        if (this.data.operation instanceof Models.ProberOperationForBACnetToReadBBMDs)
        {
            return this.data.operation;
        }

        return null;
    }

    public get configForReadDevices(): Models.ProberOperationForBACnetToReadDevices
    {
        if (this.data.operation instanceof Models.ProberOperationForBACnetToReadDevices)
        {
            return this.data.operation;
        }

        return null;
    }

    public get configForReadObjectNames(): Models.ProberOperationForBACnetToReadObjectNames
    {
        if (this.data.operation instanceof Models.ProberOperationForBACnetToReadObjectNames)
        {
            return this.data.operation;
        }

        return null;
    }

    public get configForReadObjects(): Models.ProberOperationForBACnetToReadObjects
    {
        if (this.data.operation instanceof Models.ProberOperationForBACnetToReadObjects)
        {
            return this.data.operation;
        }

        return null;
    }

    //--//

    public getLabel() { return "Configure BACnet settings"; }

    public isEnabled()
    {
        return this.data.configuringOperation && this.data.operation instanceof Models.ProberOperationForBACnet;
    }

    public isValid()
    {
        if (this.configForScanMstpTrunkForDevices)
        {
            let cfg = this.configForScanMstpTrunkForDevices;

            if (this.networksForMstpScan.length == 0) return false;

            cfg.targets = [];
            for (let el of this.networksForMstpScan)
            {
                let target = Models.ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork.newInstance({
                                                                                                             transport    : el.transport,
                                                                                                             networkNumber: el.networkNumber
                                                                                                         });
                cfg.targets.push(target);
            }
        }

        if (this.configForReadDevices)
        {
            let cfg = this.configForReadDevices;
            if (!cfg.targetDevices || cfg.targetDevices.length == 0) return false;
        }

        if (this.limitScanLow || this.limitScanHigh)
        {
            this.config.limitScan = Models.WhoIsRange.newInstance({
                                                                      low : this.limitScanLow || 0,
                                                                      high: this.limitScanHigh || ((1 << 22) - 1)
                                                                  });
        }
        else
        {
            this.config.limitScan = null;
        }

        return true;
    }

    public isNextJumpable()
    {
        return true;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
        this.limitScanLow  = this.config.limitScan?.low;
        this.limitScanHigh = this.config.limitScan?.high;

        if (!this.config.cidr)
        {
            this.config.cidr = "0.0.0.0/0";
        }

        if (!this.config.useUDP && !this.config.useEthernet)
        {
            this.config.useUDP = true;

            this.configureUdpPortDefaults();
        }

        if (this.configForReadDevices)
        {
            let cfg = this.configForReadDevices;

            cfg.targetDevices = [];
        }

        if (this.configForDiscoverRouters)
        {
            let cfg = this.configForDiscoverRouters;

            cfg.bbmds = this.populateBBMDs(cfg.bbmds);
        }

        if (this.configForDiscoverDevices)
        {
            let cfg = this.configForDiscoverDevices;

            if (cfg.broadcastRetries == undefined)
            {
                cfg.broadcastRetries = 4;
            }

            cfg.bbmds = this.populateBBMDs(cfg.bbmds);
        }

        if (this.configForReadBBMDs)
        {
            let cfg = this.configForReadBBMDs;

            if (cfg.maxRetries == undefined)
            {
                cfg.maxRetries = 4;
            }

            cfg.bbmds = this.populateBBMDs(cfg.bbmds);
        }

        if (this.configForScanMstpTrunkForDevices)
        {
            this.networksForMstpScan = [];

            let cfg = this.configForScanMstpTrunkForDevices;

            if (cfg.maxRetries == undefined)
            {
                cfg.maxRetries = 4;
            }

            if (cfg.targets)
            {
                for (let target of cfg.targets)
                {
                    let el           = new MstpScanNetwork();
                    el.transport     = target.transport;
                    el.networkNumber = target.networkNumber;

                    this.networksForMstpScan.push(el);
                }
            }
            else
            {
                for (let device of this.data.proberComponent.discoveredDevices)
                {
                    if (device.descriptor instanceof Models.BACnetDeviceDescriptor && device.routedNetworks)
                    {
                        for (let network of device.routedNetworks)
                        {
                            let el           = new MstpScanNetwork();
                            el.transport     = device.descriptor.transport;
                            el.networkNumber = network;

                            this.networksForMstpScan.push(el);
                        }
                    }
                }
            }
        }
    }

    //--//

    public configureUdpPortDefaults()
    {
        let cfg = this.config;
        if (cfg.useUDP && cfg.udpPort == undefined)
        {
            cfg.udpPort = 47808;
        }
    }

    //--//

    public addMstpScanNetwork()
    {
        this.networksForMstpScan.push(new MstpScanNetwork());

        this.detectChanges();
    }

    public removeMstpScanNetwork(network: MstpScanNetwork)
    {
        this.networksForMstpScan = this.networksForMstpScan.filter((old) => old !== network);

        this.detectChanges();
    }

    //--//

    public addBBMD()
    {
        if (!this.config.bbmds)
        {
            this.config.bbmds = [];
        }

        this.config.bbmds.push(new Models.BACnetBBMD());

        this.detectChanges();
    }

    public removeBBMD(device: Models.BACnetBBMD)
    {
        this.config.bbmds = this.config.bbmds.filter((old) => old !== device);

        this.detectChanges();
    }

    //--//

    selectBACnetDevices(selected: ProberDeviceDetails[])
    {
        let targetDevices = [];

        for (let d of selected)
        {
            if (d.descriptor instanceof Models.BACnetDeviceDescriptor)
            {
                if (d.descriptor.address)
                {
                    targetDevices.push(d.descriptor);
                }
            }
        }

        if (this.configForReadDevices)
        {
            this.configForReadDevices.targetDevices = targetDevices;
        }

        if (this.configForReadObjectNames)
        {
            this.configForReadObjectNames.targetDevices = targetDevices;
        }
    }

    selectBACnetObjects(selected: ProberObjectDetails[])
    {
        let cfg = this.configForReadObjects;
        if (cfg)
        {
            cfg.targetObjects = [];

            for (let d of selected)
            {
                if (d.device.descriptor instanceof Models.BACnetDeviceDescriptor)
                {
                    if (d.device.descriptor.address)
                    {
                        let req = Models.ProberObjectBACnet.newInstance({
                                                                            device  : d.device.descriptor,
                                                                            objectId: d.objectId
                                                                        });
                        cfg.targetObjects.push(req);
                    }
                }
            }
        }
    }

    private populateBBMDs(bbmds: Models.BACnetBBMD[]): Models.BACnetBBMD[]
    {
        if (!bbmds || bbmds.length == 0)
        {
            for (let dev of this.data.proberComponent.discoveredDevices)
            {
                if (dev.isBBMD && dev.descriptor instanceof Models.BACnetDeviceDescriptor)
                {
                    if (dev.descriptor.transport instanceof Models.UdpTransportAddress)
                    {
                        let bbmd = Models.BACnetBBMD.newInstance({
                                                                     networkAddress: dev.descriptor.transport.host,
                                                                     networkPort   : dev.descriptor.transport.port
                                                                 });

                        if (!bbmds)
                        {
                            bbmds = [];
                        }

                        bbmds.push(bbmd);
                    }
                }
            }
        }

        return bbmds;
    }
}

class MstpScanNetwork
{
    transport: Models.TransportAddress;
    networkNumber: number;

    public get host(): string
    {
        if (this.transport instanceof Models.UdpTransportAddress)
        {
            return this.transport.host;
        }

        return null;
    }

    public set host(val: string)
    {
        this.ensureUdp().host = val;
    }

    public get port(): number
    {
        if (this.transport instanceof Models.UdpTransportAddress)
        {
            return this.transport.port;
        }

        return null;
    }

    public set port(val: number)
    {
        this.ensureUdp().port = val;
    }

    public set macAddress(mac: string)
    {
        let transport = this.ensureEthernet();

        let parts = MACAddressDirective.fromMACString(mac);
        if (!parts) return;

        let [d1, d2, d3, d4, d5, d6] = parts;

        transport.d1 = d1;
        transport.d2 = d2;
        transport.d3 = d3;
        transport.d4 = d4;
        transport.d5 = d5;
        transport.d6 = d6;
    }

    public get macAddress(): string
    {
        let transport = this.ensureEthernet();
        return MACAddressDirective.toMACString(transport.d1, transport.d2, transport.d3, transport.d4, transport.d5, transport.d6);
    }

    //--//

    private ensureUdp(): Models.UdpTransportAddress
    {
        if (this.transport instanceof Models.UdpTransportAddress)
        {
            return this.transport;
        }

        let transport  = new Models.UdpTransportAddress();
        this.transport = transport;
        return transport;
    }

    private ensureEthernet(): Models.EthernetTransportAddress
    {
        if (this.transport instanceof Models.EthernetTransportAddress)
        {
            return this.transport;
        }

        let transport  = new Models.EthernetTransportAddress();
        this.transport = transport;
        return transport;
    }
}
