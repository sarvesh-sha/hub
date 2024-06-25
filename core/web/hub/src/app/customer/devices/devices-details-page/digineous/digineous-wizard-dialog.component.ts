import {Component, Inject, Injector} from "@angular/core";

import {AppContext} from "app/app.service";

import {ApiService} from "app/services/domain/api.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {BaseComponent} from "framework/ui/components";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               templateUrl: "./digineous-wizard-dialog.component.html"
           })
export class DigineousWizardDialogComponent extends BaseComponent
{
    constructor(public dialogRef: OverlayDialogRef<boolean>,
                public apis: ApiService,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: DigineousState)
    {
        super(inj);
    }

    public static open(comp: BaseComponent,
                       cfg: DigineousState): Promise<boolean>
    {
        return OverlayComponent.open(comp, DigineousWizardDialogComponent, {
            data  : cfg,
            config: OverlayConfig.newInstance({containerClasses: ["dialog-xl"]})
        });
    }

    async ngOnInit()
    {
        super.ngOnInit();
    }

    wizardCancel()
    {
        this.dialogRef.close(null);
    }

    async wizardCommit()
    {
        this.dialogRef.close(true);
    }
}

export class DigineousState
{
    public rules: Models.NormalizationRules;

    public action: DigineousWizardAction;
    public actionSub: DigineousWizardActionSub;

    public deviceTemplateId: string;
    public deviceTemplateName: string;
    public deviceTemplate: Models.DigineousDeviceLibrary;
    public deviceTemplatePristine: Models.DigineousDeviceLibrary;

    public machineTemplateId: string;
    public machineTemplateName: string;
    public machineTemplate: Models.DigineousMachineLibrary;
    public machineTemplatePristine: Models.DigineousMachineLibrary;

    public machineId: string;
    public machine: Models.DigineousMachineConfig;
    public machinePristine: Models.DigineousMachineConfig;

    public importDeviceTemplates: Models.DigineousDeviceLibrary[];
    public importMachineTemplates: Models.DigineousMachineLibrary[];

    public resetAction()
    {
        this.actionSub = null;
        this.resetActionState();
    }

    public resetActionState()
    {
        this.deviceTemplateId        = null;
        this.deviceTemplateName      = null;
        this.deviceTemplate          = null;
        this.deviceTemplatePristine  = null;
        this.machineTemplateId       = null;
        this.machineTemplateName     = null;
        this.machineTemplate         = null;
        this.machineTemplatePristine = null;
        this.machineId               = null;
        this.machine                 = null;
        this.machinePristine         = null;
    }

    public async execute(comp: BaseComponent,
                         app: AppContext)
    {
        switch (this.action)
        {
            case DigineousWizardAction.ManageDeviceTemplates:
                switch (this.actionSub)
                {
                    case DigineousWizardActionSub.Create:
                        this.deviceTemplate = await app.domain.digineous.setDeviceTemplate(this.deviceTemplate.id, this.deviceTemplate);

                        app.framework.errors.success(`Created Device Template '${this.deviceTemplate.name}'`, -1);
                        break;

                    case DigineousWizardActionSub.Edit:
                        this.deviceTemplate = await app.domain.digineous.setDeviceTemplate(this.deviceTemplate.id, this.deviceTemplate);

                        app.framework.errors.success(`Saved Device Template '${this.deviceTemplate.name}'`, -1);
                        break;

                    case DigineousWizardActionSub.Copy:
                        let oldName              = this.deviceTemplate.name;
                        this.deviceTemplate.name = this.deviceTemplateName;

                        this.deviceTemplate = await app.domain.digineous.setDeviceTemplate(this.deviceTemplate.id, this.deviceTemplate);

                        app.framework.errors.success(`Copied Device Template '${oldName}' as '${this.deviceTemplate.name}'`, -1);
                        break;

                    case DigineousWizardActionSub.Delete:
                        await app.domain.digineous.deleteDeviceTemplate(this.deviceTemplate.id);

                        app.framework.errors.success(`Deleted Device Template '${this.deviceTemplate.name}'`, -1);
                        this.deviceTemplate = null;
                        break;
                }
                break;

            case DigineousWizardAction.ManageMachineTemplates:
                switch (this.actionSub)
                {
                    case DigineousWizardActionSub.Create:
                        this.machineTemplate = await app.domain.digineous.setMachineTemplate(this.machineTemplate.id, this.machineTemplate);

                        app.framework.errors.success(`Created Machine Template '${this.machineTemplate.name}'`, -1);
                        break;

                    case DigineousWizardActionSub.Edit:
                        this.machineTemplate = await app.domain.digineous.setMachineTemplate(this.machineTemplate.id, this.machineTemplate);

                        app.framework.errors.success(`Saved Machine Template '${this.machineTemplate.name}'`, -1);
                        break;

                    case DigineousWizardActionSub.Copy:
                        let oldName               = this.machineTemplate.name;
                        this.machineTemplate.name = this.machineTemplateName;

                        this.machineTemplate = await app.domain.digineous.setMachineTemplate(this.machineTemplate.id, this.machineTemplate);

                        app.framework.errors.success(`Copied Machine Template '${oldName}' as '${this.machineTemplate.name}'`, -1);
                        break;

                    case DigineousWizardActionSub.Delete:
                        await app.domain.digineous.deleteMachineTemplate(this.machineTemplate.id);

                        app.framework.errors.success(`Deleted Machine Template '${this.machineTemplate.name}'`, -1);
                        this.machineTemplate = null;
                        break;
                }
                break;

            case DigineousWizardAction.ManageMachines:
                switch (this.actionSub)
                {
                    case DigineousWizardActionSub.Create:
                        let ri         = await app.domain.digineous.createMachine(this.machine);
                        this.machineId = ri.sysId;

                        app.framework.errors.success(`Created Machine '${this.machine.machineName}'`, -1);
                        break;

                    case DigineousWizardActionSub.Edit:
                        this.machine = await app.domain.digineous.setMachine(this.machineId, this.machine);

                        app.framework.errors.success(`Saved Machine '${this.machine.machineName}'`, -1);
                        break;

                    case DigineousWizardActionSub.Delete:
                        await app.domain.digineous.deleteMachine(this.machineId);

                        app.framework.errors.success(`Deleted Machine '${this.machine.machineName}'`, -1);
                        this.machineId = null;
                        this.machine   = null;
                        break;
                }
                break;

            case DigineousWizardAction.ImportSettings:
                if (this.importDeviceTemplates)
                {
                    for (let library of this.importDeviceTemplates)
                    {
                        await app.domain.digineous.setDeviceTemplate(library.id, library);
                    }
                }

                if (this.importMachineTemplates)
                {
                    for (let library of this.importMachineTemplates)
                    {
                        await app.domain.digineous.setMachineTemplate(library.id, library);
                    }
                }
                break;

            case DigineousWizardAction.ExportSettings:
            {
                let timestamp = MomentHelper.fileNameFormat();

                {
                    let ids       = await app.domain.digineous.getDeviceTemplateIds();
                    let libraries = await mapInParallel(ids, (id) => app.domain.digineous.getDeviceTemplate(id));

                    if (libraries.length)
                    {
                        await DownloadDialogComponent.open<Models.DigineousDeviceLibrary[]>(comp, "Export Device Templates", `device_templates__${timestamp}.json`, libraries);
                    }
                }

                {
                    let ids       = await app.domain.digineous.getMachineTemplateIds();
                    let libraries = await mapInParallel(ids, (id) => app.domain.digineous.getMachineTemplate(id));

                    if (libraries.length)
                    {
                        await DownloadDialogComponent.open<Models.DigineousMachineLibrary[]>(comp, "Export Machine Templates", `machine_templates__${timestamp}.json`, libraries);
                    }
                }

                break;
            }
        }
    }

    isDeviceLibraryReady(deviceTemplate: Models.DigineousDeviceLibrary,
                         deviceTemplatePristine?: Models.DigineousDeviceLibrary): boolean
    {
        if (!deviceTemplate) return false;

        if (!deviceTemplate.deviceFlavor || !deviceTemplate.equipmentClass) return false;
        if (UtilsService.isBlankString(deviceTemplate.name)) return false;

        if (deviceTemplatePristine)
        {
            if (UtilsService.compareJson(deviceTemplate, deviceTemplatePristine)) return false;
        }

        return true;
    }

    isMachineLibraryReady(machineTemplate: Models.DigineousMachineLibrary,
                          machineTemplatePristine?: Models.DigineousMachineLibrary): boolean
    {
        if (!machineTemplate) return false;

        if (UtilsService.isBlankString(machineTemplate.name)) return false;
        if (!machineTemplate.equipmentClass || !machineTemplate.deviceTemplates) return false;

        if (machineTemplate.deviceTemplates.length == 0) return false;
        for (let id of machineTemplate.deviceTemplates)
        {
            if (!id) return false;
        }

        if (machineTemplatePristine)
        {
            if (UtilsService.compareJson(machineTemplate, machineTemplatePristine)) return false;
        }

        return true;
    }

    isMachineReady(machineTemplate: Models.DigineousMachineLibrary,
                   machine: Models.DigineousMachineConfig,
                   machinePristine?: Models.DigineousMachineConfig): boolean
    {
        if (!machineTemplate || !machine) return false;

        if (UtilsService.isBlankString(machine.machineId)) return false;
        if (UtilsService.isBlankString(machine.machineName)) return false;

        machine.machineTemplate = machineTemplate.id;

        if (machinePristine)
        {
            if (UtilsService.compareJson(machine, machinePristine)) return false;
        }

        return true;
    }

    //--//

    setEquipmentClass(id: number): Models.WellKnownEquipmentClassOrCustom
    {
        let ecMatch = this.rules?.equipmentClasses?.find((ec) => ec.id == id);
        if (ecMatch)
        {
            let ec = new Models.WellKnownEquipmentClassOrCustom();
            if (ecMatch.wellKnown)
            {
                ec.known = ecMatch.wellKnown;
            }
            else
            {
                ec.custom = ecMatch.id;
            }

            return ec;
        }

        return undefined;
    }

    getEquipmentClass(equipmentClass: Models.WellKnownEquipmentClassOrCustom): number
    {
        if (equipmentClass?.custom)
        {
            return equipmentClass.custom;
        }

        let ecMatch = this.rules?.equipmentClasses?.find((ec) => ec.wellKnown == equipmentClass?.known);
        if (ecMatch) return ecMatch.id;

        return undefined;
    }

    //--//

    async deviceTemplatesInUse(domain: AppDomainContext): Promise<string[]>
    {
        let machineTemplates = await domain.digineous.getMachineTemplates();

        let set = new Set<string>();

        for (let machineTemplate of machineTemplates)
        {
            for (let deviceTemplate of machineTemplate.deviceTemplates)
            {
                set.add(deviceTemplate);
            }
        }

        return [...set];
    }

    async machineTemplatesInUse(domain: AppDomainContext): Promise<string[]>
    {
        let machines = await domain.digineous.getMachines();

        let set = new Set<string>();

        for (let machine of machines.values())
        {
            set.add(machine.machineTemplate);
        }

        return [...set];
    }

    //--//

    async existingDeviceTemplateNames(domain: AppDomainContext): Promise<string[]>
    {
        let deviceTemplates = await domain.digineous.getDeviceTemplates();

        return deviceTemplates.map(deviceTemplate => deviceTemplate.name);
    }

    async existingMachineTemplateNames(domain: AppDomainContext): Promise<string[]>
    {
        let machineTemplates = await domain.digineous.getMachineTemplates();

        return machineTemplates.map(deviceTemplate => deviceTemplate.name);
    }

    async existingMachineNames(domain: AppDomainContext): Promise<string[]>
    {
        let map      = await domain.digineous.getMachines();
        let machines = [...map.values()];

        return machines.map(deviceTemplate => deviceTemplate.machineName);
    }

    async existingMachineIds(domain: AppDomainContext): Promise<string[]>
    {
        let map      = await domain.digineous.getMachines();
        let machines = [...map.values()];

        return machines.map(deviceTemplate => deviceTemplate.machineId);
    }

    async existingMachineDeviceIds(domain: AppDomainContext): Promise<string[]>
    {
        let map      = await domain.digineous.getMachines();
        let machines = [...map.values()];

        let ids: string[] = [];
        for (let machine of machines)
        {
            for (let device of machine.devices)
            {
                ids.push(`${device.deviceId}`);
            }
        }

        return ids;
    }
}

export enum DigineousWizardAction
{
    ManageDeviceTemplates,
    ManageMachineTemplates,
    ManageMachines,
    ImportSettings,
    ExportSettings,
    ActiveDevices
}

export enum DigineousWizardActionSub
{
    Create,
    Edit,
    Copy,
    Delete,
}
