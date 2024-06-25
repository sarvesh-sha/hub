import {Component} from "@angular/core";

import {DigineousState, DigineousWizardAction, DigineousWizardActionSub} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-dialog.component";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-digineous-wizard-machine-templates-action-step",
               templateUrl: "./digineous-wizard-machine-templates-action.step.html",
               providers  : [
                   WizardStep.createProvider(DigineousWizardMachineTemplatesActionStep)
               ]
           })
export class DigineousWizardMachineTemplatesActionStep extends WizardStep<DigineousState>
{
    choices: ActionChoice[] =
        [
            {
                id   : DigineousWizardActionSub.Create,
                label: "Create New Machine Template"
            },
            {
                id   : DigineousWizardActionSub.Edit,
                label: "Edit Machine Template"
            },
            {
                id   : DigineousWizardActionSub.Copy,
                label: "Copy Machine Template"
            },
            {
                id   : DigineousWizardActionSub.Delete,
                label: "Delete Machine Template"
            }
        ];

    //--//

    public getLabel() { return "Select Machine Action"; }

    public isEnabled()
    {
        return this.data.action == DigineousWizardAction.ManageMachineTemplates;
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
