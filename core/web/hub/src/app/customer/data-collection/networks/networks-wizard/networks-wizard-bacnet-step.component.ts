import {Component, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";

import {NetworkWizardState} from "app/customer/data-collection/networks/networks-wizard/networks-wizard.component";
import * as Models from "app/services/proxy/model/models";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-networks-wizard-bacnet-step",
               templateUrl: "./networks-wizard-bacnet-step.component.html",
               providers  : [
                   WizardStep.createProvider(NetworksWizardBacnetStepComponent)
               ]
           })
export class NetworksWizardBacnetStepComponent extends WizardStep<NetworkWizardState>
{
    public getLabel() { return "Add BACnet Details"; }

    public isEnabled()
    {
        return this.data.bacnetEnabled;
    }

    public isValid()
    {
        return true;
    }

    public isNextJumpable()
    {
        return true;
    }

    public async onNext()
    {
        return false;
    }

    public async onStepSelected()
    {
    }
}
