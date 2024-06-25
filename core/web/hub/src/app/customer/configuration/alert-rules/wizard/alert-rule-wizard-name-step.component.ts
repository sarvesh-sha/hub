import {Component} from "@angular/core";
import {AlertRuleWizardState} from "app/customer/configuration/alert-rules/wizard/alert-rule-wizard-dialog.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-alert-rule-name-step",
               templateUrl: "./alert-rule-wizard-name-step.component.html",
               providers  : [
                   WizardStep.createProvider(AlertRuleWizardNameStepComponent)
               ]
           })
export class AlertRuleWizardNameStepComponent extends WizardStep<AlertRuleWizardState>
{
    getLabel() { return "Name & Description"; }

    isEnabled()
    {
        return true;
    }

    isValid()
    {
        if (!this.data.rules.alertDefinition) return false;
        if (!this.data.rules.alertDefinition.model.title) return false;
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
}
