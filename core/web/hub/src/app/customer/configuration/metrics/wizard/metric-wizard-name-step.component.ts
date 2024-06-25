import {Component} from "@angular/core";
import {MetricsWizardState} from "app/customer/configuration/metrics/wizard/metric-wizard-dialog.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-metric-name-step",
               templateUrl: "./metric-wizard-name-step.component.html",
               providers  : [
                   WizardStep.createProvider(MetricWizardNameStepComponent)
               ]
           })
export class MetricWizardNameStepComponent extends WizardStep<MetricsWizardState>
{
    getLabel() { return "Name & Description"; }

    isEnabled()
    {
        return true;
    }

    isValid()
    {
        if (!this.data?.metrics?.metricsDefinition?.model?.title) return false;
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
