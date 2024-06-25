import {Component} from "@angular/core";

import {ProberState} from "app/customer/data-collection/gateways/probers/probers-detail-page.component";

import * as Models from "app/services/proxy/model/models";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-gateway-probers-wizard-ipn-config-step",
               templateUrl: "./probers-wizard-ipn-config-step.component.html",
               providers  : [
                   WizardStep.createProvider(ProbersWizardIpnConfigStep)
               ]
           })
export class ProbersWizardIpnConfigStep extends WizardStep<ProberState>
{
    public get config(): Models.ProberOperationForIpn
    {
        return <Models.ProberOperationForIpn>this.data.operation;
    }

    //--//

    public getLabel() { return "Configure IPN settings"; }

    public isEnabled()
    {
        return this.data.configuringOperation && this.data.operation instanceof Models.ProberOperationForIpn;
    }

    public isValid()
    {
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
        if (this.config.ipnPort == undefined)
        {
            this.config.ipnPort = "/optio3-dev/optio3_RS485";
        }

        if (this.config.ipnBaudrate == undefined)
        {
            this.config.ipnBaudrate = 33400;
        }

        if (this.config.gpsPort == undefined)
        {
            this.config.gpsPort = "/optio3-dev/optio3_gps";
        }

        if (this.config.canPort == undefined)
        {
            this.config.canPort = "can0";
        }

        if (this.config.canFrequency == undefined)
        {
            this.config.canFrequency = 250000;
        }
    }
}
