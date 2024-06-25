import {Component} from "@angular/core";

import {ProberState} from "app/customer/data-collection/gateways/probers/probers-detail-page.component";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-gateway-probers-wizard-action-step",
               templateUrl: "./probers-wizard-action.step.html",
               providers  : [
                   WizardStep.createProvider(ProbersWizardActionStep)
               ]
           })
export class ProbersWizardActionStep extends WizardStep<ProberState>
{
    public getLabel() { return "Select Action"; }

    public isEnabled()
    {
        return true;
    }

    public isValid()
    {
        if (this.data.action == null) return false; // Not loaded yet...

        return true;
    }

    public isNextJumpable()
    {
        return this.data.action != null;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
    }
}
