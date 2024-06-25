import {Component, Type} from "@angular/core";
import {ProberState} from "app/customer/data-collection/gateways/probers/probers-detail-page.component";

import * as Models from "app/services/proxy/model/models";
import {WizardStep} from "framework/ui/wizards/wizard-step";
import {Memoizer} from "framework/utils/memoizers";

@Component({
               selector   : "o3-gateway-probers-wizard-protocol-step",
               templateUrl: "./probers-wizard-protocol.step.html",
               providers  : [
                   WizardStep.createProvider(ProbersWizardProtocolStep)
               ]
           })
export class ProbersWizardProtocolStep extends WizardStep<ProberState>
{
    @Memoizer
    public getProtocolTypesList(): { type: Type<Models.ProberOperation>, title: string }[]
    {
        let res = [];

        res.push({
                     type : Models.ProberOperationForBACnet,
                     title: "BACnet"
                 });

        res.push({
                     type : Models.ProberOperationForIpn,
                     title: "IPN"
                 });

        res.push({
                     type : Models.ProberOperationForCANbus,
                     title: "CAN bus"
                 });

        return res;
    }

    //--//

    public getLabel() { return "Select Protocol"; }

    public isEnabled()
    {
        return this.data.configuringOperation;
    }

    public isValid()
    {
        if (!this.data.protocolType) return false; // Not loaded yet...

        return true;
    }

    public isNextJumpable()
    {
        return this.data.protocolType != null;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
    }
}
