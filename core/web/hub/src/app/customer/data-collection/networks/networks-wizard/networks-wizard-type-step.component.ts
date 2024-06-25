import {Component} from "@angular/core";

import {NetworkWizardState} from "app/customer/data-collection/networks/networks-wizard/networks-wizard.component";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-networks-wizard-type-step",
               templateUrl: "./networks-wizard-type-step.component.html",
               providers  : [
                   WizardStep.createProvider(NetworksWizardTypeStepComponent)
               ]
           })
export class NetworksWizardTypeStepComponent extends WizardStep<NetworkWizardState>
{
    public getLabel() { return "Type"; }

    public isEnabled()
    {
        return true;
    }

    public isValid()
    {
        if (!this.data.bacnetEnabled && !this.data.ipnEnabled) return false;

        this.data.ensureProtocolConfig();

        return true;
    }

    public isNextJumpable()
    {
        if (!this.data.bacnetEnabled && !this.data.ipnEnabled) return false;

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
