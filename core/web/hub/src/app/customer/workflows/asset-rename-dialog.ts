import {Component, Inject, Injector} from "@angular/core";
import {WorkflowDialogBase, WorkflowDialogConfig} from "app/customer/workflows/workflow-dialog-base";
import {AssetExtended, DeviceElementExtended, DeviceExtended, LogicalAssetExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {WorkflowExtended} from "app/services/domain/events.service";
import * as Models from "app/services/proxy/model/models";
import {DatatableManager} from "framework/ui/datatables/datatable-manager";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

@Component({
               template: `
                   <o3-standard-form label="Rename"
                                     primaryButtonText="Save"
                                     secondaryButtonText="Cancel"
                                     (submitted)="save()"
                                     (cancelled)="dialogRef.close(false)">
                       <mat-form-field>
                           <input name="oldName" placeholder="Old Name" disabled matInput [ngModel]="data.assets[0].typedModel.name"/>
                       </mat-form-field>
                       <mat-form-field>
                           <input name="name" placeholder="New Name" matInput [(ngModel)]="name" required/>
                       </mat-form-field>
                   </o3-standard-form>`
           })
export class AssetRenameDialog extends WorkflowDialogBase
{
    name: string;

    constructor(inj: Injector,
                public dialogRef: OverlayDialogRef<boolean>,
                @Inject(OVERLAY_DATA) public data: WorkflowDialogConfig<AssetExtended>)
    {
        super(inj);
        this.name = data.assets[0].typedModel.name;
    }

    public static open(comp: BaseApplicationComponent,
                       clickedId: string,
                       table: DatatableManager<Models.RecordIdentity, any, any>): Promise<boolean>
    {
        return super.openDialog(AssetExtended, comp, AssetRenameDialog, clickedId, table);
    }

    async save()
    {
        let workflowDetails: Models.WorkflowDetails;
        let workflowType: Models.WorkflowType;

        let assetName = "";

        let assets = this.data.assets.map((asset) => Models.WorkflowAsset.newInstance({
                                                                                          sysId: asset.typedModel.sysId,
                                                                                          name : asset.typedModel.name
                                                                                      }));
        if (this.data.assets[0] instanceof DeviceElementExtended)
        {
            workflowDetails = this.getWorkflowForControlPoint(assets);
            workflowType    = Models.WorkflowType.RenameControlPoint;
            assetName       = "control point";
        }
        else if (this.data.assets[0] instanceof LogicalAssetExtended)
        {
            workflowDetails = this.getWorkflowForEquipment(assets);
            workflowType    = Models.WorkflowType.RenameEquipment;
            assetName       = "equipment";
        }
        else if (this.data.assets[0] instanceof DeviceExtended)
        {
            workflowDetails = this.getWorkflowForDevice(assets);
            workflowType    = Models.WorkflowType.RenameDevice;
            assetName       = "device";
        }

        if (!workflowDetails)
        {
            this.dialogRef.close(false);
            return;
        }

        let workflowExtended = WorkflowExtended.create(this.app.domain.events,
                                                       workflowType,
                                                       workflowDetails,
                                                       `Renaming ${this.data.assets.length} ${assetName}${this.data.assets.length && "s" || ""} to "${this.name}"`);

        await this.app.domain.workflows.create(workflowExtended.typedModel);

        for (let asset of this.data.assets)
        {
            asset.typedModel.name = this.name;
            asset.refreshDisplayName();
        }

        this.dialogRef.close(true);
    }

    private getWorkflowForDevice(assets: Models.WorkflowAsset[])
    {
        return Models.WorkflowDetailsForRenameDevice.newInstance({
                                                                     devices      : assets,
                                                                     deviceNewName: this.name
                                                                 });

    }

    private getWorkflowForEquipment(assets: Models.WorkflowAsset[])
    {
        return Models.WorkflowDetailsForRenameEquipment.newInstance({
                                                                        equipments      : assets,
                                                                        equipmentNewName: this.name
                                                                    });

    }

    private getWorkflowForControlPoint(assets: Models.WorkflowAsset[])
    {
        return Models.WorkflowDetailsForRenameControlPoint.newInstance({
                                                                           controlPoints      : assets.map((asset) => asset.sysId),
                                                                           controlPointNewName: this.name
                                                                       });

    }
}
