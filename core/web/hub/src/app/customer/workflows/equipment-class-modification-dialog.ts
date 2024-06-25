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
                   <o3-standard-form label="Change equipment class"
                                     primaryButtonText="Save"
                                     secondaryButtonText="Cancel"
                                     (submitted)="save()"
                                     (cancelled)="dialogRef.close(false)">
                       <mat-form-field>
                           <o3-select name="oldEquipmentClass"
                                      placeholder="Old Equipment Class"
                                      [o3EquipmentClassSelector]="true"
                                      [ngModel]="oldEquipmentClassId" disabled></o3-select>
                       </mat-form-field>
                       <mat-form-field>
                           <o3-select name="equipmentClass"
                                      placeholder="New Equipment Class"
                                      [o3EquipmentClassSelector]="true"
                                      [(ngModel)]="equipmentClassId" required></o3-select>
                       </mat-form-field>

                   </o3-standard-form>`
           })
export class EquipmentClassModificationDialog extends WorkflowDialogBase
{
    oldEquipmentClassId: string;
    equipmentClassId: string;

    constructor(inj: Injector,
                public dialogRef: OverlayDialogRef<boolean>,
                @Inject(OVERLAY_DATA) public data: WorkflowDialogConfig<LogicalAssetExtended>)
    {
        super(inj);
        this.equipmentClassId    = data.assets[0].typedModel.equipmentClassId;
        this.oldEquipmentClassId = this.equipmentClassId;
    }

    public static open(comp: BaseApplicationComponent,
                       clickedId: string,
                       table: DatatableManager<Models.RecordIdentity, any, any>): Promise<boolean>
    {
        return super.openDialog(LogicalAssetExtended, comp, EquipmentClassModificationDialog, clickedId, table);
    }

    async save()
    {
        let equipments = this.data.assets.map((eq) => Models.WorkflowAsset.newInstance({
                                                                                           sysId: eq.typedModel.sysId,
                                                                                           name : eq.typedModel.name
                                                                                       }));

        let workflowExtended = WorkflowExtended.create(this.app.domain.events,
                                                       Models.WorkflowType.SetEquipmentClass,
                                                       Models.WorkflowDetailsForSetEquipmentClass.newInstance({
                                                                                                                  equipments      : equipments,
                                                                                                                  equipmentClassId: this.equipmentClassId
                                                                                                              }),
                                                       `Setting equipment class for ${equipments.length} equipment`);

        await this.app.domain.workflows.create(workflowExtended.typedModel);

        for (let equipment of this.data.assets)
        {
            equipment.typedModel.equipmentClassId = this.equipmentClassId;
        }

        this.dialogRef.close(true);
    }
}
