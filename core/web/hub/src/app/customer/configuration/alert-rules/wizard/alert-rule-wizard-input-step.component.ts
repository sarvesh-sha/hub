import {Component} from "@angular/core";
import {AlertRuleWizardState} from "app/customer/configuration/alert-rules/wizard/alert-rule-wizard-dialog.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-alert-rule-input-step",
               templateUrl: "./alert-rule-wizard-input-step.component.html",
               providers  : [
                   WizardStep.createProvider(AlertRuleWizardInputStepComponent)
               ]
           })
export class AlertRuleWizardInputStepComponent extends WizardStep<AlertRuleWizardState>
{
    getLabel() { return "Inputs"; }

    isEnabled()
    {
        return this.data.rules.userProgramDetails && this.data.rules.userProgramDetails.data.getInputParameters().length > 0;
    }

    isValid()
    {
        return true;
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

    public get inputs()
    {
        return this.data.rules.userProgramDetails && this.data.rules.userProgramDetails.data.getInputParameters() || [];
    }
}
