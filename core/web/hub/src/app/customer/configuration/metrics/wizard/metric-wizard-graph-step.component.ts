import {Component} from "@angular/core";
import {MetricsWizardState} from "app/customer/configuration/metrics/wizard/metric-wizard-dialog.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-metric-wizard-graph-step",
               templateUrl: "./metric-wizard-graph-step.component.html",
               providers  : [
                   WizardStep.createProvider(MetricWizardGraphStepComponent)
               ]
           })
export class MetricWizardGraphStepComponent extends WizardStep<MetricsWizardState>
{
    getLabel() { return "Data"; }

    isEnabled()
    {
        return true;
    }

    isValid()
    {
        if (!this.data?.metrics?.details?.graph?.isValid(false)) return false;
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

