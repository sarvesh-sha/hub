import {Component} from "@angular/core";

import {DigineousState, DigineousWizardAction, DigineousWizardActionSub} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-dialog.component";
import {AppDomainContext} from "app/services/domain/domain.module";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-digineous-wizard-machines-delete-step",
               templateUrl: "./digineous-wizard-machines-delete.step.html",
               providers  : [
                   WizardStep.createProvider(DigineousWizardMachinesDeleteStep)
               ]
           })
export class DigineousWizardMachinesDeleteStep extends WizardStep<DigineousState>
{
    public getLabel() { return "Delete Machine"; }

    public isEnabled()
    {
        return this.data.action == DigineousWizardAction.ManageMachines && this.data.actionSub == DigineousWizardActionSub.Delete;
    }

    public isValid()
    {
        return !!this.data.machine;
    }

    public isNextJumpable()
    {
        return this.isValid();
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
        this.data.resetActionState();
    }

    //--//

    public async selectMachine()
    {
        if (this.data.machineId)
        {
            let domain = this.wizard.injector.get(AppDomainContext);

            this.data.machine = await domain.digineous.getMachine(this.data.machineId);
        }
    }
}
