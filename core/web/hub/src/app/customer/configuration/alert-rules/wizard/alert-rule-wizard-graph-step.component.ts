import {Component} from "@angular/core";
import {AlertRuleWizardState} from "app/customer/configuration/alert-rules/wizard/alert-rule-wizard-dialog.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-alert-rule-graph-step",
               templateUrl: "./alert-rule-wizard-graph-step.component.html",
               providers  : [
                   WizardStep.createProvider(AlertRuleWizardGraphStepComponent)
               ]
           })
export class AlertRuleWizardGraphStepComponent extends WizardStep<AlertRuleWizardState>
{
    getLabel() { return "Data"; }

    isEnabled()
    {
        return true;
    }

    isValid()
    {
        if (!this.data?.rules?.details?.graph?.isValid(true)) return false;
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

