import {Component} from "@angular/core";
import {UUID} from "angular2-uuid";

import {DigineousState, DigineousWizardAction, DigineousWizardActionSub} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-dialog.component";
import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-digineous-wizard-machine-templates-create-step",
               templateUrl: "./digineous-wizard-machine-templates-create.step.html",
               providers  : [
                   WizardStep.createProvider(DigineousWizardMachineTemplatesCreateStep)
               ]
           })
export class DigineousWizardMachineTemplatesCreateStep extends WizardStep<DigineousState>
{
    notAllowed: string[];

    //--//

    public getLabel() { return "New Machine Template"; }

    public isEnabled()
    {
        return this.data.action == DigineousWizardAction.ManageMachineTemplates && this.data.actionSub == DigineousWizardActionSub.Create;
    }

    public isValid()
    {
        return this.data.isMachineLibraryReady(this.data.machineTemplate);
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

        let domain      = this.wizard.injector.get(AppDomainContext);
        this.notAllowed = await this.data.existingMachineTemplateNames(domain);

        let obj             = new Models.DigineousMachineLibrary();
        obj.id              = UUID.UUID();
        obj.deviceTemplates = [];

        this.data.machineTemplate = obj;
    }

    set equipmentClass(id: number)
    {
        this.data.machineTemplate.equipmentClass = this.data.setEquipmentClass(id);
    }

    get equipmentClass(): number
    {
        return this.data.getEquipmentClass(this.data.machineTemplate?.equipmentClass);
    }

    //--//

    addDevice()
    {
        this.data.machineTemplate.deviceTemplates.push(null);

        this.detectChanges();
    }

    removeDevice(index: number)
    {
        this.data.machineTemplate.deviceTemplates.splice(index, 1);

        this.detectChanges();
    }
}
