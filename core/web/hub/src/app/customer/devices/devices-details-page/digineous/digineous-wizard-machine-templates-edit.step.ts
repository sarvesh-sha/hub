import {Component} from "@angular/core";

import {DigineousState, DigineousWizardAction, DigineousWizardActionSub} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-dialog.component";
import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-digineous-wizard-machine-templates-edit-step",
               templateUrl: "./digineous-wizard-machine-templates-edit.step.html",
               providers  : [
                   WizardStep.createProvider(DigineousWizardMachineTemplatesEditStep)
               ]
           })
export class DigineousWizardMachineTemplatesEditStep extends WizardStep<DigineousState>
{
    notAllowed: string[];

    //--//

    public getLabel() { return "Edit Machine Template"; }

    public isEnabled()
    {
        return this.data.action == DigineousWizardAction.ManageMachineTemplates && this.data.actionSub == DigineousWizardActionSub.Edit;
    }

    public isValid()
    {
        return this.data.isMachineLibraryReady(this.data.machineTemplate, this.data.machineTemplatePristine);
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

    public async selectTemplate()
    {
        if (this.data.machineTemplateId)
        {
            let domain = this.wizard.injector.get(AppDomainContext);

            this.data.machineTemplate         = await domain.digineous.getMachineTemplate(this.data.machineTemplateId);
            this.data.machineTemplatePristine = Models.DigineousMachineLibrary.deepClone(this.data.machineTemplate);

            this.notAllowed = await this.data.existingMachineTemplateNames(domain);
        }
    }

    set equipmentClass(id: number)
    {
        if (this.data.machineTemplate)
        {
            this.data.machineTemplate.equipmentClass = this.data.setEquipmentClass(id);
        }
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
