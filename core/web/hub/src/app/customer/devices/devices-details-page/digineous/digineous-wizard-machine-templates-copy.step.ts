import {Component} from "@angular/core";
import {UUID} from "angular2-uuid";

import {DigineousState, DigineousWizardAction, DigineousWizardActionSub} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-dialog.component";
import {AppDomainContext} from "app/services/domain/domain.module";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-digineous-wizard-machine-templates-copy-step",
               templateUrl: "./digineous-wizard-machine-templates-copy.step.html",
               providers  : [
                   WizardStep.createProvider(DigineousWizardMachineTemplatesCopyStep)
               ]
           })
export class DigineousWizardMachineTemplatesCopyStep extends WizardStep<DigineousState>
{
    notAllowed: string[];

    //--//

    public getLabel() { return "Copy Machine Template"; }

    public isEnabled()
    {
        return this.data.action == DigineousWizardAction.ManageMachineTemplates && this.data.actionSub == DigineousWizardActionSub.Copy;
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

        let domain      = this.wizard.injector.get(AppDomainContext);
        this.notAllowed = await this.data.existingMachineTemplateNames(domain);
    }

    //--//

    public async selectTemplate()
    {
        if (this.data.machineTemplateId)
        {
            let domain = this.wizard.injector.get(AppDomainContext);

            this.data.machineTemplate    = await domain.digineous.getMachineTemplate(this.data.machineTemplateId);
            this.data.machineTemplate.id = UUID.UUID();
        }
    }
}
