import {Component} from "@angular/core";
import {UUID} from "angular2-uuid";

import {DigineousState, DigineousWizardAction, DigineousWizardActionSub} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-dialog.component";
import {AppDomainContext} from "app/services/domain/domain.module";
import {UtilsService} from "framework/services/utils.service";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-digineous-wizard-device-templates-copy-step",
               templateUrl: "./digineous-wizard-device-templates-copy.step.html",
               providers  : [
                   WizardStep.createProvider(DigineousWizardDeviceTemplatesCopyStep)
               ]
           })
export class DigineousWizardDeviceTemplatesCopyStep extends WizardStep<DigineousState>
{
    notAllowed: string[];

    //--//

    public getLabel() { return "Copy Device Template"; }

    public isEnabled()
    {
        return this.data.action == DigineousWizardAction.ManageDeviceTemplates && this.data.actionSub == DigineousWizardActionSub.Copy;
    }

    public isValid()
    {
        return !!this.data.deviceTemplate && !UtilsService.isBlankString(this.data.deviceTemplateName);
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
        this.notAllowed = await this.data.existingDeviceTemplateNames(domain);
    }

    //--//

    public async selectTemplate()
    {
        if (this.data.deviceTemplateId)
        {
            let domain = this.wizard.injector.get(AppDomainContext);

            this.data.deviceTemplate    = await domain.digineous.getDeviceTemplate(this.data.deviceTemplateId);
            this.data.deviceTemplate.id = UUID.UUID();
        }
    }
}
