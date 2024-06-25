import {Component} from "@angular/core";

import {DigineousState, DigineousWizardAction, DigineousWizardActionSub} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-dialog.component";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-digineous-wizard-device-templates-action-step",
               templateUrl: "./digineous-wizard-device-templates-action.step.html",
               providers  : [
                   WizardStep.createProvider(DigineousWizardDeviceTemplatesActionStep)
               ]
           })
export class DigineousWizardDeviceTemplatesActionStep extends WizardStep<DigineousState>
{
    choices: ActionChoice[] =
        [
            {
                id   : DigineousWizardActionSub.Create,
                label: "Create New Device Template"
            },
            {
                id   : DigineousWizardActionSub.Edit,
                label: "Edit Device Template"
            },
            {
                id   : DigineousWizardActionSub.Copy,
                label: "Copy Device Template"
            },
            {
                id   : DigineousWizardActionSub.Delete,
                label: "Delete Device Template"
            }
        ];

    //--//

    public getLabel() { return "Select Device Action"; }

    public isEnabled()
    {
        return this.data.action == DigineousWizardAction.ManageDeviceTemplates;
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
        this.data.resetActionState();
    }
}

interface ActionChoice
{
    id: DigineousWizardActionSub;
    label: string;
}
