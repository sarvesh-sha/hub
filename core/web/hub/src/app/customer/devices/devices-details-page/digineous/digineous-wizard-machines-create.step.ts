import {Component} from "@angular/core";

import {DigineousState, DigineousWizardAction, DigineousWizardActionSub} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-dialog.component";
import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-digineous-wizard-machines-create-step",
               templateUrl: "./digineous-wizard-machines-create.step.html",
               providers  : [
                   WizardStep.createProvider(DigineousWizardMachinesCreateStep)
               ]
           })
export class DigineousWizardMachinesCreateStep extends WizardStep<DigineousState>
{
    notAllowedId: string[];
    notAllowedName: string[];
    notAllowedDeviceId: string[];
    pairs: Pair[] = [];

    //--//

    public getLabel() { return "New Machine"; }

    public isEnabled()
    {
        return this.data.action == DigineousWizardAction.ManageMachines && this.data.actionSub == DigineousWizardActionSub.Create;
    }

    public isValid()
    {
        return this.data.isMachineReady(this.data.machineTemplate, this.data.machine);
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

        let domain              = this.wizard.injector.get(AppDomainContext);
        this.notAllowedId       = await this.data.existingMachineIds(domain);
        this.notAllowedName     = await this.data.existingMachineNames(domain);
        this.notAllowedDeviceId = await this.data.existingMachineDeviceIds(domain);
    }

    //--//

    public async selectTemplate()
    {
        if (this.data.machineTemplateId)
        {
            let domain = this.wizard.injector.get(AppDomainContext);

            let machineTemplate = await domain.digineous.getMachineTemplate(this.data.machineTemplateId);
            if (machineTemplate?.id != this.data.machineTemplate?.id)
            {
                this.data.machine = null;
            }

            this.data.machineTemplate = machineTemplate;

            if (!this.data.machine || this.data.machine.devices?.length != this.data.machineTemplate?.deviceTemplates?.length)
            {
                let obj             = new Models.DigineousMachineConfig();
                obj.machineTemplate = this.data.machineTemplateId;
                obj.devices         = [];

                this.pairs = [];

                for (let deviceTemplateId of this.data.machineTemplate.deviceTemplates)
                {
                    let cfg = new Models.DigineousDeviceConfig();
                    obj.devices.push(cfg);

                    let deviceTemplate = await domain.digineous.getDeviceTemplate(deviceTemplateId);
                    this.pairs.push(new Pair(deviceTemplate, cfg));
                }

                this.data.machine = obj;
            }
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
