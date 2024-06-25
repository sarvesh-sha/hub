import {Component} from "@angular/core";
import {NetworkWizardState} from "app/customer/data-collection/networks/networks-wizard/networks-wizard.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-networks-wizard-confirm-step",
               templateUrl: "./networks-wizard-confirm-step.component.html",
               styles     : [".slide-toggle { padding-top: 25px; }"],
               providers  : [
                   WizardStep.createProvider(NetworksWizardConfirmStepComponent)
               ]
           })
export class NetworksWizardConfirmStepComponent extends WizardStep<NetworkWizardState>
{
    public getLabel() { return "Confirm"; }

    public isEnabled()
    {
        return true;
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
