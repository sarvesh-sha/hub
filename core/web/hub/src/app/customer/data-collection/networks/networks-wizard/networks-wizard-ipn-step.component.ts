import {Component, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";

import {NetworkWizardState} from "app/customer/data-collection/networks/networks-wizard/networks-wizard.component";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-networks-wizard-ipn-step",
               templateUrl: "./networks-wizard-ipn-step.component.html",
               styles     : [".slide-toggle { padding-top: 25px; }"],
               providers  : [
                   WizardStep.createProvider(NetworksWizardIpnStepComponent)
               ]
           })
export class NetworksWizardIpnStepComponent extends WizardStep<NetworkWizardState>
{
    public getLabel() { return "Add IPN Details"; }

    public isEnabled()
    {
        return this.data.ipnEnabled;
    }

    public isValid()
    {
        return true;
    }

    public isNextJumpable()
    {
        return true;
    }

    public async onNext()
    {
        return false;
    }

    public async onStepSelected()
    {
    }
}
