import {Component} from "@angular/core";

import {ProberState} from "app/customer/data-collection/gateways/probers/probers-detail-page.component";

import * as Models from "app/services/proxy/model/models";
import {MACAddressDirective} from "framework/directives/mac-address.directive";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-gateway-probers-wizard-new-device-step",
               templateUrl: "./probers-wizard-new-device.step.html",
               providers  : [
                   WizardStep.createProvider(ProbersWizardNewDeviceStep)
               ]
           })
export class ProbersWizardNewDeviceStep extends WizardStep<ProberState>
{
    useUDP: boolean = true;

    networkNumber: number;
    instanceNumber: number;
    ipAddress: string;
    ipPort: number = 47808;

    macAddress: string;

    mstpNetworkNumber: number;
    mstpNetworkAddress: number;

    //--//

    public getLabel() { return "Add Device"; }

    public isEnabled()
    {
        return this.data.configuringDevice;
    }

    public isValid()
    {
        // Both present, or both missing.
        if ((this.instanceNumber != undefined) != (this.networkNumber != undefined)) return false;

        // Both present, or both missing.
        if ((this.mstpNetworkNumber != undefined) != (this.mstpNetworkAddress != undefined)) return false;

        let t: Models.TransportAddress;
        if (!this.useUDP)
        {
            let parsed = MACAddressDirective.fromMACString(this.macAddress);
            if (parsed)
            {
                t = Models.EthernetTransportAddress.newInstance({
                                                                    d1: parsed[0],
                                                                    d2: parsed[1],
                                                                    d3: parsed[2],
                                                                    d4: parsed[3],
                                                                    d5: parsed[4],
                                                                    d6: parsed[5]
                                                                });
            }
        }
        else
        {
            t = Models.UdpTransportAddress.newInstance({
                                                           host: this.ipAddress,
                                                           port: this.ipPort
                                                       });
        }

        let d = Models.BACnetDeviceDescriptor.newInstance({transport: t});

        if (this.mstpNetworkAddress)
        {
            d.bacnetAddress = Models.BACnetAddress.newInstance({
                                                                   network_number: this.mstpNetworkNumber,
                                                                   mac_address   : ProbersWizardNewDeviceStep.toHexString(this.mstpNetworkAddress)
                                                               });
        }

        if (this.instanceNumber)
        {
            d.address = Models.BACnetDeviceAddress.newInstance({
                                                                   networkNumber : this.networkNumber,
                                                                   instanceNumber: this.instanceNumber
                                                               });
        }

        this.data.newDevice = d;

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
    }

    private static toHexString(num: number): string
    {
        let high = (num / 16) & 0xF;
        let low  = (num & 0xF);

        return high.toString(16) + low.toString(16);
    }
}
