import {Component, Inject, Injector} from "@angular/core";
import {WorkflowDialogBase, WorkflowDialogConfig} from "app/customer/workflows/workflow-dialog-base";
import {LogicalAssetExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {WorkflowExtended} from "app/services/domain/events.service";

import * as Models from "app/services/proxy/model/models";
import {DatatableManager} from "framework/ui/datatables/datatable-manager";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

@Component({
               template: `
                   <o3-standard-form label="Add child equipment"
                                     primaryButtonText="Save"
                                     secondaryButtonText="Cancel"
                                     (submitted)="save()"
                                     (cancelled)="dialogRef.close(false)">
                       <mat-form-field>
                           <input matInput placeholder="New Equipment Name" name="name" [(ngModel)]="name" required/>
                       </mat-form-field>
                       <mat-form-field>
                           <o3-select name="location" [o3LocationSelector]="true" placeholder="New Location" [(ngModel)]="locationSysId" [multiSelect]="false">
                           </o3-select>
                       </mat-form-field>
                       <mat-form-field>
                           <o3-select name="equipmentClass"
                                      placeholder="New Equipment Class"
                                      [o3EquipmentClassSelector]="true"
                                      [(ngModel)]="equipmentClassId"></o3-select>
                       </mat-form-field>
                   </o3-standard-form>`
           })
export class ChildEquipmentDialog extends WorkflowDialogBase
{
    name: string;

    locationSysId: string;

    equipmentClassId: string;

    constructor(inj: Injector,
                public dialogRef: OverlayDialogRef<boolean>,
                @Inject(OVERLAY_DATA) public data: WorkflowDialogConfig<LogicalAssetExtended>)
    {
        super(inj);
    }

    public static async open(comp: BaseApplicationComponent,
                             clickedId: string,
                             table: DatatableManager<Models.RecordIdentity, any, any>): Promise<boolean>
    {
        return super.openDialog(LogicalAssetExtended, comp, ChildEquipmentDialog, clickedId, table);
    }

    async save()
    {

        let parent      = this.data.assets[0];
        let parentAsset = Models.WorkflowAsset.newInstance({
                                                               sysId: parent.model.sysId,
                                                               name : parent.model.name
                                                           });


        let description = `Creating child equipment "${this.name}" for ${parentAsset.name}`;

        let workflowExtended = WorkflowExtended.create(this.app.domain.events,
                                                       Models.WorkflowType.NewEquipment,
                                                       Models.WorkflowDetailsForNewEquipment.newInstance({
                                                                                                             equipmentName   : this.name,
                                                                                                             locationSysId   : this.locationSysId,
                                                                                                             equipmentClassId: this.equipmentClassId,
                                                                                                             parentEquipment : parentAsset
                                                                                                         }),
                                                       description);

        await this.app.domain.workflows.create(workflowExtended.typedModel);

        this.dialogRef.close(true);
    }
}
