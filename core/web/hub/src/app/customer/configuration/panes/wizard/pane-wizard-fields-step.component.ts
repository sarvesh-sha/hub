import {Component} from "@angular/core";
import {PaneWizardState} from "app/customer/configuration/panes/wizard/pane-wizard-dialog.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-pane-wizard-fields-step",
               templateUrl: "./pane-wizard-fields-step.component.html",
               providers  : [
                   WizardStep.createProvider(PaneWizardFieldsStepComponent)
               ]
           })
export class PaneWizardFieldsStepComponent extends WizardStep<PaneWizardState>
{
    getLabel() { return "Fields"; }

    isEnabled()
    {
        return true;
    }

    isValid()
    {
        return !!this.data?.pane?.elements?.length;
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

