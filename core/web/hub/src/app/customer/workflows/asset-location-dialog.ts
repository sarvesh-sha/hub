import {Component, Inject, Injector} from "@angular/core";
import {WorkflowDialogBase, WorkflowDialogConfig} from "app/customer/workflows/workflow-dialog-base";
import {AssetExtended, DeviceExtended, LogicalAssetExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {WorkflowExtended} from "app/services/domain/events.service";
import * as Models from "app/services/proxy/model/models";
import {DatatableManager} from "framework/ui/datatables/datatable-manager";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

@Component({
               template: `
                   <o3-standard-form label="Set Location" primaryButtonText="Save" (submitted)="save()" [showSecondary]="false">
                       <mat-form-field>
                           <o3-select name="oldLocation" [o3LocationSelector]="true" placeholder="Old Location" disabled [multiSelect]="false"
                                      [ngModel]="locationSysIdOld">
                           </o3-select>
                       </mat-form-field>
                       <mat-form-field>
                           <o3-select name="location" [o3LocationSelector]="true" placeholder="New Location" [(ngModel)]="locationSysId" [multiSelect]="false" required>
                           </o3-select>
                       </mat-form-field>
                   </o3-standard-form>`
           })
export class AssetLocationDialog extends WorkflowDialogBase
{
    locationSysIdOld: string;
    locationSysId: string;

    constructor(inj: Injector,
                public dialogRef: OverlayDialogRef<boolean>,
                @Inject(OVERLAY_DATA) public data: WorkflowDialogConfig<AssetExtended>)
    {
        super(inj);
        this.locationSysIdOld = data.assets[0].typedModel.location?.sysId;
        this.locationSysId = this.locationSysIdOld;
    }

    public static open(comp: BaseApplicationComponent,
                       clickedId: string,
                       table: DatatableManager<Models.RecordIdentity, any, any>): Promise<boolean>
    {
        return super.openDialog(AssetExtended, comp, AssetLocationDialog, clickedId, table);
    }

    async save()
    {
        let workflowDetails: Models.WorkflowDetails;
        let workflowType: Models.WorkflowType;

        if (this.data.assets[0] instanceof LogicalAssetExtended)
        {
            workflowDetails = this.getWorkflowForEquipment();
            workflowType    = Models.WorkflowType.SetEquipmentLocation;
        }
        else if (this.data.assets[0] instanceof DeviceExtended)
        {
            workflowDetails = this.getWorkflowForDevice();
            workflowType    = Models.WorkflowType.SetDeviceLocation;
        }

        if (!workflowDetails)
        {
            this.dialogRef.close(false);
            return;
        }

        let workflowExtended = WorkflowExtended.create(this.app.domain.events,
                                                       workflowType,
                                                       workflowDetails);

        await this.app.domain.workflows.create(workflowExtended.typedModel);

        for (let asset of this.data.assets)
        {
            asset.typedModel.location = LogicalAssetExtended.newIdentity(this.locationSysId);
        }

        this.dialogRef.close(true);
    }

    private getWorkflowForDevice()
    {
        return Models.WorkflowDetailsForSetDeviceLocation.newInstance({
                                                                          devices      : this.data.assets.map((device) => Models.WorkflowAsset.newInstance({
                                                                                                                                                               sysId: device.typedModel.sysId,
                                                                                                                                                               name : device.typedModel.name
                                                                                                                                                           })),
                                                                          locationSysId: this.locationSysId
                                                                      });

    }

    private getWorkflowForEquipment()
    {
        return Models.WorkflowDetailsForSetEquipmentLocation.newInstance({
                                                                             equipments   : this.data.assets.map((eq) => Models.WorkflowAsset.newInstance({
                                                                                                                                                              sysId: eq.typedModel.sysId,
                                                                                                                                                              name : eq.typedModel.name
                                                                                                                                                          })),
                                                                             locationSysId: this.locationSysId
                                                                         });

    }
}
