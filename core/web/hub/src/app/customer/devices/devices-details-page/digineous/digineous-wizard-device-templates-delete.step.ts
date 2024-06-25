import {Component} from "@angular/core";

import {DigineousState, DigineousWizardAction, DigineousWizardActionSub} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-dialog.component";
import {AppDomainContext} from "app/services/domain/domain.module";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-digineous-wizard-device-templates-delete-step",
               templateUrl: "./digineous-wizard-device-templates-delete.step.html",
               providers  : [
                   WizardStep.createProvider(DigineousWizardDeviceTemplatesDeleteStep)
               ]
           })
export class DigineousWizardDeviceTemplatesDeleteStep extends WizardStep<DigineousState>
{
    inUse: string[];

    //--//

    public getLabel() { return "Delete Device Template"; }

    public isEnabled()
    {
        return this.data.action == DigineousWizardAction.ManageDeviceTemplates && this.data.actionSub == DigineousWizardActionSub.Delete;
    }

    public isValid()
    {
        return !!this.data.deviceTemplate;
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
        this.inUse = await this.data.deviceTemplatesInUse(domain);
    }

    //--//

    public async selectTemplate()
    {
        if (this.data.deviceTemplateId)
        {
            let domain = this.wizard.injector.get(AppDomainContext);

            this.data.deviceTemplate = await domain.digineous.getDeviceTemplate(this.data.deviceTemplateId);
        }
    }
}
