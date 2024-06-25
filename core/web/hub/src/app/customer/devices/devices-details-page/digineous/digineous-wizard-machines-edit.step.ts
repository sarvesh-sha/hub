import {Component} from "@angular/core";

import {DigineousState, DigineousWizardAction, DigineousWizardActionSub} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-dialog.component";
import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-digineous-wizard-machines-edit-step",
               templateUrl: "./digineous-wizard-machines-edit.step.html",
               providers  : [
                   WizardStep.createProvider(DigineousWizardMachinesEditStep)
               ]
           })
export class DigineousWizardMachinesEditStep extends WizardStep<DigineousState>
{
    notAllowedId: string[];
    notAllowedName: string[];
    notAllowedDeviceId: string[];
    pairs: Pair[] = [];

    //--//

    public getLabel() { return "Edit Machine"; }

    public isEnabled()
    {
        return this.data.action == DigineousWizardAction.ManageMachines && this.data.actionSub == DigineousWizardActionSub.Edit;
    }

    public isValid()
    {
        return this.data.isMachineReady(this.data.machineTemplate, this.data.machine, this.data.machinePristine);
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

            this.data.machine         = await domain.digineous.getMachine(this.data.machineId);
            this.data.machinePristine = Models.DigineousMachineConfig.deepClone(this.data.machine);
            this.data.machineTemplate = await domain.digineous.getMachineTemplate(this.data.machine.machineTemplate);

            this.pairs = [];

            let numDevices = this.data.machineTemplate?.deviceTemplates?.length || 0;
            if (numDevices != this.data.machine?.devices?.length)
            {
                this.data.machineId = null;
                this.data.machine   = null;
            }
            else
            {
                for (let i = 0; i < numDevices; i++)
                {
                    let deviceTemplate = await domain.digineous.getDeviceTemplate(this.data.machineTemplate.deviceTemplates[i]);
                    this.pairs.push(new Pair(deviceTemplate, this.data.machine.devices[i]));
                }
            }

            this.notAllowedId       = await this.data.existingMachineIds(domain);
            this.notAllowedName     = await this.data.existingMachineNames(domain);
            this.notAllowedDeviceId = await this.data.existingMachineDeviceIds(domain);
        }
    }
}

class Pair
{
    readonly isBlackBox: boolean;

    constructor(public readonly template: Models.DigineousDeviceLibrary,
                public readonly config: Models.DigineousDeviceConfig)
    {
        this.isBlackBox = template?.deviceFlavor == Models.DigineousDeviceFlavor.BlackBox;
    }
}
