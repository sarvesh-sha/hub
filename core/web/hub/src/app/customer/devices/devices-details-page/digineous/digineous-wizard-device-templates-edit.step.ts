import {Component, ViewChild} from "@angular/core";

import {DigineousDeviceLibraryComponent} from "app/customer/devices/devices-details-page/digineous/digineous-device-library.component";
import {DigineousState, DigineousWizardAction, DigineousWizardActionSub} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-dialog.component";

import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-digineous-wizard-device-templates-edit-step",
               templateUrl: "./digineous-wizard-device-templates-edit.step.html",
               providers  : [
                   WizardStep.createProvider(DigineousWizardDeviceTemplatesEditStep)
               ]
           })
export class DigineousWizardDeviceTemplatesEditStep extends WizardStep<DigineousState>
{
    @ViewChild(DigineousDeviceLibraryComponent) libraryComponent: DigineousDeviceLibraryComponent;

    notAllowed: string[];

    //--//

    public getLabel() { return "Edit Device Template"; }

    public isEnabled()
    {
        return this.data.action == DigineousWizardAction.ManageDeviceTemplates && this.data.actionSub == DigineousWizardActionSub.Edit;
    }

    public isValid()
    {
        return this.libraryComponent?.ready && this.data.isDeviceLibraryReady(this.data.deviceTemplate, this.data.deviceTemplatePristine);
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
        if (this.data.deviceTemplateId)
        {
            let domain = this.wizard.injector.get(AppDomainContext);

            this.data.deviceTemplate         = await domain.digineous.getDeviceTemplate(this.data.deviceTemplateId);
            this.data.deviceTemplatePristine = Models.DigineousDeviceLibrary.deepClone(this.data.deviceTemplate);

            this.notAllowed = await this.data.existingDeviceTemplateNames(domain);
        }
    }

    set equipmentClass(id: number)
    {
        if (this.data.deviceTemplate)
        {
            this.data.deviceTemplate.equipmentClass = this.data.setEquipmentClass(id);
        }
    }

    get equipmentClass(): number
    {
        return this.data.getEquipmentClass(this.data.deviceTemplate?.equipmentClass);
    }

    set deviceFlavor(flavor: Models.DigineousDeviceFlavor)
    {
        if (this.data.deviceTemplate?.deviceFlavor != flavor)
        {
            this.updateTemplate(flavor);
        }
    }

    get deviceFlavor(): Models.DigineousDeviceFlavor
    {
        return this.data.deviceTemplate?.deviceFlavor;
    }

    private async updateTemplate(flavor: Models.DigineousDeviceFlavor)
    {
        let domain = this.wizard.injector.get(AppDomainContext);

        let deviceTemplate = await domain.digineous.newDeviceTemplate(flavor);

        if (this.data.deviceTemplate?.name)
        {
            deviceTemplate.name = this.data.deviceTemplate?.name;
        }

        if (this.data.deviceTemplate?.equipmentClass)
        {
            deviceTemplate.equipmentClass = this.data.deviceTemplate?.equipmentClass;
        }

        this.data.deviceTemplate = deviceTemplate;
    }
}
