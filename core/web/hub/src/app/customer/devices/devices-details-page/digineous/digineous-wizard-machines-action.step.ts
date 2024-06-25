import {Component} from "@angular/core";

import {DigineousState, DigineousWizardAction, DigineousWizardActionSub} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-dialog.component";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-digineous-wizard-machines-action-step",
               templateUrl: "./digineous-wizard-machines-action.step.html",
               providers  : [
                   WizardStep.createProvider(DigineousWizardMachinesActionStep)
               ]
           })
export class DigineousWizardMachinesActionStep extends WizardStep<DigineousState>
{
    choices: ActionChoice[] =
        [
            {
                id   : DigineousWizardActionSub.Create,
                label: "Create New Machine"
            },
            {
                id   : DigineousWizardActionSub.Edit,
                label: "Edit Machine"
            },
            {
                id   : DigineousWizardActionSub.Delete,
                label: "Delete Machine"
            }
        ];

    //--//

    public getLabel() { return "Select Machine Action"; }

    public isEnabled()
    {
        return this.data.action == DigineousWizardAction.ManageMachines;
    }

    public isValid()
    {
        if (this.data.actionSub == null) return false; // Not loaded yet...

        return true;
    }

    public isNextJumpable()
    {
        return this.data.actionSub != null;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
    }
}

interface ActionChoice
{
    id: DigineousWizardActionSub;
    label: string;
}
