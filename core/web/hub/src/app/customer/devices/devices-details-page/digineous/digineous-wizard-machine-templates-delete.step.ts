import {Component} from "@angular/core";

import {DigineousState, DigineousWizardAction, DigineousWizardActionSub} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-dialog.component";
import {AppDomainContext} from "app/services/domain/domain.module";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-digineous-wizard-machine-templates-delete-step",
               templateUrl: "./digineous-wizard-machine-templates-delete.step.html",
               providers  : [
                   WizardStep.createProvider(DigineousWizardMachineTemplatesDeleteStep)
               ]
           })
export class DigineousWizardMachineTemplatesDeleteStep extends WizardStep<DigineousState>
{
    inUse: string[];

    //--//

    public getLabel() { return "Delete Machine Template"; }

    public isEnabled()
    {
        return this.data.action == DigineousWizardAction.ManageMachineTemplates && this.data.actionSub == DigineousWizardActionSub.Delete;
    }

    public isValid()
    {
        return !!this.data.machineTemplate;
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

        let domain = this.wizard.injector.get(AppDomainContext);
        this.inUse = await this.data.machineTemplatesInUse(domain);
    }

    //--//

    public async selectTemplate()
    {
        if (this.data.machineTemplateId)
        {
            let domain = this.wizard.injector.get(AppDomainContext);

            this.data.machineTemplate = await domain.digineous.getMachineTemplate(this.data.machineTemplateId);
        }
    }
}
