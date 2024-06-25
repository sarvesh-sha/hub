import {Component, Input, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";
import {AssetExtended, LogicalAssetExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {WorkflowExtended} from "app/services/domain/events.service";
import {ControlPointsSelectionExtended, EquipmentSelectionExtended} from "app/services/domain/report-definitions.service";
import {WorkflowDetailsExtended} from "app/services/domain/workflows.service";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-workflow-details-editor",
               styles     : [".mat-icon-button { font-size: 14px; }"],
               templateUrl: "./workflow-details-editor.component.html"
           })
export class WorkflowDetailsEditorComponent extends BaseApplicationComponent
{
    @ViewChild("detailsForm") form: NgForm;

    detailsForAssignControlPointsToEquipment: Models.WorkflowDetailsForAssignControlPointsToEquipment;
    detailsForIgnoreDevice: Models.WorkflowDetailsForIgnoreDevice;
    detailsForMergeEquipments: Models.WorkflowDetailsForMergeEquipments;
    detailsForNewEquipment: Models.WorkflowDetailsForNewEquipment;
    detailsForRemoveEquipment: Models.WorkflowDetailsForRemoveEquipment;
    detailsForRenameControlPoint: Models.WorkflowDetailsForRenameControlPoint;
    detailsForRenameDevice: Models.WorkflowDetailsForRenameDevice;
    detailsForRenameEquipment: Models.WorkflowDetailsForRenameEquipment;
    detailsForSetControlPointsClass: Models.WorkflowDetailsForSetControlPointsClass;
    detailsForSetDeviceLocation: Models.WorkflowDetailsForSetDeviceLocation;
    detailsForSetEquipmentClass: Models.WorkflowDetailsForSetEquipmentClass;
    detailsForSetEquipmentLocation: Models.WorkflowDetailsForSetEquipmentLocation;
    detailsForSetEquipmentParent: Models.WorkflowDetailsForSetEquipmentParent;

    detailsExtended: WorkflowDetailsExtended<any>;

    controlPointSelection: ControlPointsSelectionExtended;

    equipmentSelection: EquipmentSelectionExtended;
    equipmentSelection2: EquipmentSelectionExtended;
    equipmentSelectionList: EquipmentSelectionExtended;
    settingSecondaryEquipment = false;
    settingEquipmentList      = false;

    private m_data: WorkflowExtended;

    @Input()
    public set data(data: WorkflowExtended)
    {
        this.m_data = data;
        if (this.m_data)
        {
            this.init();
        }
    }

    public get data(): WorkflowExtended
    {
        return this.m_data;
    }

    @Input()
    public readonly: boolean = false;

    private init()
    {
        if (!this.data.typedModel.details || !this.data.typedModel.type)
        {
            return;
        }

        this.detailsForAssignControlPointsToEquipment = this.data.getDetails(Models.WorkflowDetailsForAssignControlPointsToEquipment);
        this.detailsForIgnoreDevice                   = this.data.getDetails(Models.WorkflowDetailsForIgnoreDevice);
        this.detailsForMergeEquipments                = this.data.getDetails(Models.WorkflowDetailsForMergeEquipments);
        this.detailsForNewEquipment                   = this.data.getDetails(Models.WorkflowDetailsForNewEquipment);
        this.detailsForRemoveEquipment                = this.data.getDetails(Models.WorkflowDetailsForRemoveEquipment);
        this.detailsForRenameControlPoint             = this.data.getDetails(Models.WorkflowDetailsForRenameControlPoint);
        this.detailsForRenameDevice                   = this.data.getDetails(Models.WorkflowDetailsForRenameDevice);
        this.detailsForRenameEquipment                = this.data.getDetails(Models.WorkflowDetailsForRenameEquipment);
        this.detailsForSetControlPointsClass          = this.data.getDetails(Models.WorkflowDetailsForSetControlPointsClass);
        this.detailsForSetDeviceLocation              = this.data.getDetails(Models.WorkflowDetailsForSetDeviceLocation);
        this.detailsForSetEquipmentClass              = this.data.getDetails(Models.WorkflowDetailsForSetEquipmentClass);
        this.detailsForSetEquipmentLocation           = this.data.getDetails(Models.WorkflowDetailsForSetEquipmentLocation);
        this.detailsForSetEquipmentParent             = this.data.getDetails(Models.WorkflowDetailsForSetEquipmentParent);

        this.detailsExtended = WorkflowDetailsExtended.newInstance(this.data.typedModel.type, this.data.typedModel.details);

        this.initControlPointsSelection();
        this.initEquipmentSelection();
    }

    isValid(): boolean
    {
        return this.form && this.form.valid;
    }

    navigateToDevice(sysId: string)
    {
        if (sysId)
        {
            this.app.ui.navigation.go("/devices/device", [sysId]);
        }
    }

    navigateToEquipment(sysId: string)
    {
        if (sysId)
        {
            this.app.ui.navigation.go("/equipment/equipment", [sysId]);
        }
    }

    initEquipmentSelection()
    {
        this.settingSecondaryEquipment = false;
        this.settingEquipmentList      = false;
        let equipments: string[]       = [];
        let equipments2: string[]      = [];
        let equipmentsList: string[]   = [];

        let equipment1    = this.detailsExtended.getPrimaryEquipment();
        let equipment2    = this.detailsExtended.getSecondaryEquipment();
        let equipmentList = this.detailsExtended.getEquipmentList();

        if (equipment1)
        {
            equipments = [equipment1.id];
        }

        if (equipment2)
        {
            equipments2 = [equipment2.id];
        }

        if (equipmentList)
        {
            equipmentsList = equipmentList.map((eq) => eq.id);
        }

        this.equipmentSelection     = new EquipmentSelectionExtended(this.app.domain, this.mapToIdentities(equipments));
        this.equipmentSelection2    = new EquipmentSelectionExtended(this.app.domain, this.mapToIdentities(equipments2));
        this.equipmentSelectionList = new EquipmentSelectionExtended(this.app.domain, this.mapToIdentities(equipmentsList));
    }

    async saveEquipmentSelections()
    {
        let settingSecondary = this.settingSecondaryEquipment;
        let settingList      = this.settingEquipmentList;

        let selection = this.equipmentSelection.identities;
        if (settingSecondary) selection = this.equipmentSelection2.identities;
        if (settingList) selection = this.equipmentSelectionList.identities;

        if (selection && selection.length === 1)
        {
            let id    = selection[0].sysId;
            let equip = await this.app.domain.assets.getTypedExtendedById(LogicalAssetExtended, id);
            let name  = equip.model.name;

            if (!settingSecondary)
            {
                this.detailsExtended.setPrimaryEquipment(id, name);
            }
            else
            {
                this.detailsExtended.setSecondaryEquipment(id, name);
            }
        }
        else if (selection)
        {
            let equipments = await this.app.domain.assets.getExtendedBatch(selection);
            this.detailsExtended.setEquipmentList(equipments.map((eq) =>
                                                                 {
                                                                     return {
                                                                         id  : eq.model.sysId,
                                                                         name: eq.model.name
                                                                     };
                                                                 }));
        }

        this.initEquipmentSelection();
    }

    initControlPointsSelection()
    {
        let points = this.detailsExtended.getControlPointSelection();

        if (points)
        {
            let selectionModel = Models.ControlPointsSelection.newInstance({
                                                                               identities: this.mapToIdentities(points)
                                                                           });

            this.controlPointSelection = new ControlPointsSelectionExtended(this.app.domain, selectionModel);
        }
    }

    saveControlPointsSelection()
    {
        this.detailsExtended.setControlPointSelection(this.controlPointSelection.model.identities.map((id) => id.sysId));
        this.initControlPointsSelection();
    }

    private mapToIdentities(ids: string[]): Models.RecordIdentity[]
    {
        return ids.filter((id) => !!id)
                  .map((id) => AssetExtended.newIdentityRaw(id));
    }
}
