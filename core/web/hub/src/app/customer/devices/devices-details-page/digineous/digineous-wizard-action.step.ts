import {Component} from "@angular/core";

import {DigineousState, DigineousWizardAction} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-dialog.component";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-digineous-wizard-action-step",
               templateUrl: "./digineous-wizard-action.step.html",
               providers  : [
                   WizardStep.createProvider(DigineousWizardActionStep)
               ]
           })
export class DigineousWizardActionStep extends WizardStep<DigineousState>
{
    choices: ActionChoice[] =
        [
            {
                id   : DigineousWizardAction.ManageMachines,
                label: "Manage Machines"
            },
            {
                id   : DigineousWizardAction.ManageMachineTemplates,
                label: "Manage Machine Templates"
            },
            {
                id   : DigineousWizardAction.ManageDeviceTemplates,
                label: "Manage Device Templates"
            },
            {
                id   : DigineousWizardAction.ActiveDevices,
                label: "List Active Devices"
            },
            {
                id   : DigineousWizardAction.ImportSettings,
                label: "Import Settings"
            },
            {
                id   : DigineousWizardAction.ExportSettings,
                label: "Export Settings"
            }
        ];

    //--//

    public getLabel() { return "Select Action"; }

    public isEnabled()
    {
        return true;
    }

    public isValid()
    {
        if (this.data.action == null) return false; // Not loaded yet...

        return true;
    }

    public isNextJumpable()
    {
        return this.data.action != null;
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
    id: DigineousWizardAction;
    label: string;
}
