import {Component} from "@angular/core";

import {PaneWizardState} from "app/customer/configuration/panes/wizard/pane-wizard-dialog.component";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-pane-wizard-name-step",
               templateUrl: "./pane-wizard-name-step.component.html",
               providers  : [WizardStep.createProvider(PaneWizardNameStepComponent)]
           })
export class PaneWizardNameStepComponent extends WizardStep<PaneWizardState>
{
    getLabel() { return "Name"; }

    isEnabled()
    {
        return true;
    }

    isValid()
    {
        return !!this.data?.pane?.model?.name;
    }

    isNextJumpable()
    {
        return true;
    }

    async onNext()
    {
        return false;
    }

    public async onStepSelected()
    {
    }
}
