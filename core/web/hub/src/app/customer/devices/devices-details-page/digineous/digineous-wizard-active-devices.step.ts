import {Component} from "@angular/core";
import {AppContext} from "app/app.service";

import {DigineousState, DigineousWizardAction} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-dialog.component";
import {AppDomainContext} from "app/services/domain/domain.module";

import * as Models from "app/services/proxy/model/models";
import {ImportDialogComponent, ImportHandler} from "framework/ui/dialogs/import-dialog.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-digineous-wizard-active-devices-step",
               templateUrl: "./digineous-wizard-active-devices.step.html",
               providers  : [
                   WizardStep.createProvider(DigineousWizardActiveDevicesStep)
               ]
           })
export class DigineousWizardActiveDevicesStep extends WizardStep<DigineousState>
{
    activeDevices: { deviceId: number, lastActivity: Date }[] = [];

    public getLabel() { return "Active Devices"; }

    public isEnabled()
    {
        return this.data.action == DigineousWizardAction.ActiveDevices;
    }

    public isValid()
    {
        if (this.data.importDeviceTemplates == null && this.data.importMachineTemplates == null) return false; // Not loaded yet...

        return true;
    }

    public isNextJumpable()
    {
        return this.data.actionSub != null;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
        let domain = this.wizard.injector.get(AppDomainContext);

        this.activeDevices = await domain.digineous.getActiveDevices();
    }

    //--//

    public async triggerDeviceImport()
    {
        let app = this.wizard.injector.get(AppContext);

        this.data.importDeviceTemplates = await ImportDialogComponent.open(this, "Import Device Templates", new DigineousDeviceLibraryImportHandler(app.domain));
    }

    public async triggerMachineImport()
    {
        let app = this.wizard.injector.get(AppContext);

        this.data.importMachineTemplates = await ImportDialogComponent.open(this, "Import Machine Templates", new DigineousMachineLibraryImportHandler(app.domain));
    }
}

export class DigineousDeviceLibraryImportHandler implements ImportHandler<Models.DigineousDeviceLibrary[]>
{
    constructor(private domain: AppDomainContext)
    {
    }

    returnRawBlobs(): boolean
    {
        return false;
    }

    async parseFile(result: string): Promise<Models.DigineousDeviceLibrary[]>
    {
        return JSON.parse(result);
    }
}

export class DigineousMachineLibraryImportHandler implements ImportHandler<Models.DigineousMachineLibrary[]>
{
    constructor(private domain: AppDomainContext)
    {
    }

    returnRawBlobs(): boolean
    {
        return false;
    }

    async parseFile(result: string): Promise<Models.DigineousMachineLibrary[]>
    {
        return JSON.parse(result);
    }
}
