import {Component} from "@angular/core";
import {AppContext} from "app/app.service";

import {DigineousState, DigineousWizardAction} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-dialog.component";
import {AppDomainContext} from "app/services/domain/domain.module";

import * as Models from "app/services/proxy/model/models";
import {ImportDialogComponent, ImportHandler} from "framework/ui/dialogs/import-dialog.component";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-digineous-wizard-import-step",
               templateUrl: "./digineous-wizard-import.step.html",
               providers  : [
                   WizardStep.createProvider(DigineousWizardImportStep)
               ]
           })
export class DigineousWizardImportStep extends WizardStep<DigineousState>
{
    public getLabel() { return "Select Import Action"; }

    public isEnabled()
    {
        return this.data.action == DigineousWizardAction.ImportSettings;
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
