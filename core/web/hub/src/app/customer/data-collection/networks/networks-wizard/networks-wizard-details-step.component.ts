import {Component, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";
import {NetworkWizardState} from "app/customer/data-collection/networks/networks-wizard/networks-wizard.component";
import {ComponentFrameworkContext} from "framework/ui/components";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-networks-wizard-details-step",
               templateUrl: "./networks-wizard-details-step.component.html",
               providers  : [
                   WizardStep.createProvider(NetworksWizardDetailsStepComponent)
               ]
           })
export class NetworksWizardDetailsStepComponent extends WizardStep<NetworkWizardState>
{
    public getLabel() { return "Add Network Details"; }

    public isEnabled()
    {
        return true;
    }

    public isValid()
    {
        if (!this.data.network) return false; // Not loaded yet...

        if (!this.data.networkModel.samplingPeriod || this.data.networkModel.samplingPeriod < 1) return false;

        if (!this.data.locationId) return false;

        return true;
    }

    public isNextJumpable()
    {
        return this.data.location != null;
    }

    public async onNext()
    {
        try
        {
            await this.data.initLocation();
            return false;
        }
        catch (e)
        {
            this.inject(ComponentFrameworkContext)
                .errors
                .error("UNEXPECTED_ERROR", "An error occurred loading catalog information.");
            return true;
        }
    }

    public async onStepSelected()
    {
    }
}
