import {Component} from "@angular/core";
import {LocationWizardState} from "app/customer/configuration/locations/wizard/location-wizard-dialog.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-location-wizard-info-step",
               templateUrl: "./location-wizard-info-step.component.html",
               providers  : [
                   WizardStep.createProvider(LocationWizardInfoStep)
               ]
           })
export class LocationWizardInfoStep extends WizardStep<LocationWizardState>
{
    public getLabel() { return "Name & Type"; }

    public isEnabled()
    {
        return true;
    }

    public isValid()
    {
        return this.isNextJumpable();
    }

    public isNextJumpable()
    {
        return this.data.model.name != null && this.data.model.type != null;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
    }
}
